package com.clinicahumana.clinica_humana.config;

import com.clinicahumana.clinica_humana.model.AreaClinica;
import com.clinicahumana.clinica_humana.model.Empleado;
import com.clinicahumana.clinica_humana.model.Medicamento;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Tutor;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.EmpleadoRepository;
import com.clinicahumana.clinica_humana.repository.MedicamentoRepository;
import com.clinicahumana.clinica_humana.repository.PacienteRepository;
import com.clinicahumana.clinica_humana.repository.TutorRepository;
import com.clinicahumana.clinica_humana.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Siembra las cuentas iniciales del sistema en una base de datos limpia de MySQL.
 * Se ejecuta al arrancar la aplicación y solo crea usuarios que todavía no existen
 * (idempotente: no duplica ni sobreescribe cuentas existentes).
 *
 * Las contraseñas se guardan codificadas con el PasswordEncoder de la aplicación
 * (DelegatingPasswordEncoder, que usa BCrypt internamente y produce hashes {bcrypt}...).
 *
 * IMPORTANTE: en producción debe cambiarse la contraseña tras el primer acceso.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Clave de las cuentas de demostración (coincide con la ayuda mostrada en login.html). */
    private static final String CLAVE_DEMO = "123456";

    /**
     * Nombre oficial de la especialista odontopediatra que debe aparecer siempre
     * con la titulación completa en el panel de la doctora y en toda la comunicación del módulo CISO.
     */
    public static final String NOMBRE_OFICIAL_DRA_GONZALEZ_HERRERA =
            "DRA. GLORIA GONZÁLEZ HERRERA - CIRUJANA DENTISTA";

    /** Clave del administrador: más robusta por ser el rol con acceso total. */
    private static final String CLAVE_ADMIN = "Admin@2026";

    private final UsuarioRepository usuarioRepository;
    private final MedicamentoRepository medicamentoRepo;
    private final PacienteRepository pacienteRepo;
    private final TutorRepository tutorRepo;
    private final CitaRepository citaRepo;
    private final EmpleadoRepository empleadoRepo;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           MedicamentoRepository medicamentoRepo,
                           PacienteRepository pacienteRepo,
                           TutorRepository tutorRepo,
                           CitaRepository citaRepo,
                           EmpleadoRepository empleadoRepo,
                           JdbcTemplate jdbcTemplate,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.medicamentoRepo = medicamentoRepo;
        this.pacienteRepo = pacienteRepo;
        this.tutorRepo = tutorRepo;
        this.citaRepo = citaRepo;
        this.empleadoRepo = empleadoRepo;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("=== Clínica Humana: verificando usuarios iniciales ===");

        // 1. Administrador general (panel, personal, reportes)
        crearUsuarioSiNoExiste("admin", "Administrador General", "ADMIN", CLAVE_ADMIN);

        // 2. Recepción y caja (cobros y agenda de odontopediatría)
        crearUsuarioSiNoExiste("recepcion", "Recepción y Caja", "RECEPCIONISTA", CLAVE_DEMO);

        // 3. Médicas pediatras
        crearUsuarioSiNoExiste("pilar.rios", "Dra. Pilar Ríos", "PEDIATRA", CLAVE_DEMO);
        crearUsuarioSiNoExiste("laura.mendez", "Dra. Laura Méndez", "PEDIATRA", CLAVE_DEMO);

        // 4. Médicas odontopediatras
        crearUsuarioSiNoExiste("gloria.gonzalez", NOMBRE_OFICIAL_DRA_GONZALEZ_HERRERA, "ODONTOPEDIATRA", CLAVE_DEMO);
        crearUsuarioSiNoExiste("sofia.castro", "Dra. Sofía Castro", "ODONTOPEDIATRA", CLAVE_DEMO);

        log.info("=== Usuarios iniciales listos | admin: '{}' | cuentas demo: '{}' ===",
                CLAVE_ADMIN, CLAVE_DEMO);

        // Catálogo base de medicamentos (Vademécum interno)
        sembrarMedicamentos();

        // Separación de pacientes y tutores por área clínica
        migrarAreasClinicas();

        // WhatsApp de contacto de las especialistas (editable después en /empleados)
        asegurarContactosWhatsApp();
    }

    /**
     * Registra el número de WhatsApp de cada especialista si su expediente laboral todavía
     * no lo tiene. Son los teléfonos publicados en los documentos de la clínica y pueden
     * corregirse desde el módulo de empleados sin tocar el código.
     */
    private void asegurarContactosWhatsApp() {
        Map<String, String> contactos = new HashMap<>();
        contactos.put("gloria.gonzalez", "7474990538"); // Odontología / CISO
        contactos.put("pilar.rios", "7471432709");      // Pediatría

        for (Map.Entry<String, String> contacto : contactos.entrySet()) {
            usuarioRepository.findByUsername(contacto.getKey()).ifPresent(usuario ->
                    empleadoRepo.findByUsuarioIdUsuario(usuario.getIdUsuario()).ifPresent(empleado -> {
                        String actual = empleado.getTelefono();
                        if (actual == null || actual.isBlank()) {
                            empleado.setTelefono(contacto.getValue());
                            empleadoRepo.save(empleado);
                            log.info("WhatsApp de contacto asignado a '{}'.", contacto.getKey());
                        }
                    }));
        }
    }

    /**
     * Deja lista la separación de paneles (odontología / pediatría) sobre una base ya existente:
     *
     * 1. La CURP era única en todo el sistema; ahora es única dentro de cada área, porque una misma
     *    persona puede registrarse por separado en odontología y en pediatría.
     * 2. A los pacientes y tutores anteriores se les asigna el área que corresponde a su historial
     *    (pediatría sólo si su historial es exclusivamente pediátrico; en cualquier otro caso, dental).
     *
     * Es idempotente: si ya está todo migrado no hace nada.
     */
    private void migrarAreasClinicas() {
        try {
            List<String> indices = jdbcTemplate.queryForList(
                    "SELECT INDEX_NAME FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pacientes' "
                            + "AND COLUMN_NAME = 'curp' AND NON_UNIQUE = 0 AND SEQ_IN_INDEX = 1",
                    String.class);

            if (!indices.isEmpty() && !indices.contains("uk_paciente_curp_area")) {
                Integer compuesto = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.STATISTICS "
                                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pacientes' "
                                + "AND INDEX_NAME = 'uk_paciente_curp_area' AND SEQ_IN_INDEX = 2",
                        Integer.class);
                if (compuesto == null || compuesto == 0) {
                    jdbcTemplate.execute("ALTER TABLE pacientes "
                            + "ADD CONSTRAINT uk_paciente_curp_area UNIQUE (curp, area)");
                }
                for (String indice : indices) {
                    if (!"uk_paciente_curp_area".equalsIgnoreCase(indice)) {
                        jdbcTemplate.execute("ALTER TABLE pacientes DROP INDEX `" + indice + "`");
                        log.info("Índice único global de CURP '{}' eliminado: ahora es único por área.", indice);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo ajustar el índice único de CURP por área: {}", e.getMessage());
        }

        Map<Long, AreaClinica> areaPorTutor = new HashMap<>();
        int pacientesMigrados = 0;

        for (Paciente paciente : pacienteRepo.findAll()) {
            if (paciente.getArea() != null) {
                if (paciente.getTutor() != null && paciente.getTutor().getIdTutor() != null) {
                    areaPorTutor.putIfAbsent(paciente.getTutor().getIdTutor(), paciente.getArea());
                }
                continue;
            }

            Long id = paciente.getIdPaciente();
            boolean soloPediatria = citaRepo.existsByPacienteIdPacienteAndEspecialidadIn(id, List.of("PEDIATRIA"))
                    && !citaRepo.existsByPacienteIdPacienteAndEspecialidadIn(id,
                            List.of("ODONTOLOGIA", "ODONTOPEDIATRIA"));
            AreaClinica area = soloPediatria ? AreaClinica.PEDIATRIA : AreaClinica.ODONTOLOGIA;

            paciente.setArea(area);
            pacienteRepo.save(paciente);
            pacientesMigrados++;

            if (paciente.getTutor() != null && paciente.getTutor().getIdTutor() != null) {
                areaPorTutor.putIfAbsent(paciente.getTutor().getIdTutor(), area);
            }
        }

        int tutoresMigrados = 0;
        for (Tutor tutor : tutorRepo.findAll()) {
            if (tutor.getArea() != null) {
                continue;
            }
            tutor.setArea(areaPorTutor.getOrDefault(tutor.getIdTutor(), AreaClinica.ODONTOLOGIA));
            tutorRepo.save(tutor);
            tutoresMigrados++;
        }

        if (pacientesMigrados > 0 || tutoresMigrados > 0) {
            log.info("Áreas clínicas asignadas a {} paciente(s) y {} tutor(es) previos.",
                    pacientesMigrados, tutoresMigrados);
        }
    }

    /**
     * Siembra el catálogo inicial de fármacos (Pediatría, Odontopediatría y uso general).
     * Idempotente: omite fármacos cuyo nombre comercial ya existe.
     * Se puede ampliar desde el módulo /medicamentos o importando el Excel de la clínica.
     */
    private void sembrarMedicamentos() {
        // {nombreComercial, sustanciaActiva, presentacion, concentracion, categoria}
        String[][] catalogo = {
            // De uso general (ambas especialidades)
            {"Paracetamol", "Paracetamol", "Solución oral", "120 mg/5 ml", "GENERAL"},
            {"Ibuprofeno", "Ibuprofeno", "Suspensión oral", "100 mg/5 ml", "GENERAL"},
            // Pediatría
            {"Amoxicilina", "Amoxicilina", "Suspensión oral", "250 mg/5 ml", "PEDIATRIA"},
            {"Azitromicina", "Azitromicina", "Suspensión oral", "200 mg/5 ml", "PEDIATRIA"},
            {"Cefalexina", "Cefalexina", "Suspensión oral", "250 mg/5 ml", "PEDIATRIA"},
            {"Loratadina", "Loratadina", "Solución oral", "5 mg/5 ml", "PEDIATRIA"},
            {"Prednisolona", "Prednisolona", "Solución oral", "3 mg/ml", "PEDIATRIA"},
            {"Omeprazol", "Omeprazol", "Cápsula", "20 mg", "PEDIATRIA"},
            {"Nistatina", "Nistatina", "Suspensión oral", "100,000 UI/ml", "PEDIATRIA"},
            {"Salbutamol", "Salbutamol", "Solución para inhalación", "5 mg/ml", "PEDIATRIA"},
            {"SRO (Suero Oral)", "Sales de rehidratación oral", "Polvo para solución", "26.4 g/L", "PEDIATRIA"},
            // Odontopediatría
            {"Lidocaína 2%", "Lidocaína con epinefrina", "Solución inyectable", "2% (1:80,000)", "ODONTOLOGIA"},
            {"Clorexidina 0.12%", "Clorexidina digluconato", "Enjuague bucal", "0.12%", "ODONTOLOGIA"},
            {"Flúor barniz", "Flúoruro de sodio", "Barniz tópico", "5%", "ODONTOLOGIA"},
            {"Flúor gel", "Flúoruro de sodio", "Gel tópico para aplicación", "1.23%", "ODONTOLOGIA"},
            {"Nitrato de plata", "Nitrato de plata", "Solución tópica", "25%", "ODONTOLOGIA"},
            {"Amoxicilina/clavulanato", "Amoxicilina + clavulanato de potasio", "Suspensión oral", "400/57 mg/5 ml", "ODONTOLOGIA"},
            {"Metronidazol", "Metronidazol", "Suspensión oral", "250 mg/5 ml", "ODONTOLOGIA"},
            {"Hidróxido de calcio", "Hidróxido de calcio", "Pasta pulpocida", "Uso profesional", "ODONTOLOGIA"}
        };

        int creados = 0;
        for (String[] f : catalogo) {
            java.util.Optional<Medicamento> existente = medicamentoRepo.findByNombreComercialIgnoreCase(f[0]);
            if (existente.isPresent()) {
                // Normaliza la categoría legada al valor canónico usado por el backend
                Medicamento m = existente.get();
                if ("ODONTOPEDIATRIA".equalsIgnoreCase(m.getCategoria())) {
                    m.setCategoria("ODONTOLOGIA");
                    medicamentoRepo.save(m);
                }
                continue;
            }
            Medicamento m = new Medicamento();
            m.setNombreComercial(f[0]);
            m.setSustanciaActiva(f[1]);
            m.setPresentacion(f[2]);
            m.setConcentracion(f[3]);
            m.setCategoria(f[4]);
            medicamentoRepo.save(m);
            creados++;
        }

        if (creados > 0) {
            log.info("Catálogo de medicamentos: {} fármacos sembrados.", creados);
        } else {
            log.info("Catálogo de medicamentos: ya estaba poblado; nada que sembrar.");
        }
    }

    private void crearUsuarioSiNoExiste(String username, String nombreCompleto, String rol, String passwordPlano) {
        if (usuarioRepository.existsByUsername(username)) {
            log.info("El usuario '{}' ya existe en la BD; se omite.", username);
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setEmail(username.replace(".", "") + "@clinicahumana.mx");
        usuario.setRol(rol);
        usuario.setPassword(passwordEncoder.encode(passwordPlano)); // hash {bcrypt}...
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        log.info("Usuario creado -> username: '{}', rol: '{}'", username, rol);
    }
}
