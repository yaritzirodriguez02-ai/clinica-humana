package com.clinicahumana.clinica_humana.config;

import com.clinicahumana.clinica_humana.model.Medicamento;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.MedicamentoRepository;
import com.clinicahumana.clinica_humana.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           MedicamentoRepository medicamentoRepo,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.medicamentoRepo = medicamentoRepo;
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
            {"Lidocaína 2%", "Lidocaína con epinefrina", "Solución inyectable", "2% (1:80,000)", "ODONTOPEDIATRIA"},
            {"Clorexidina 0.12%", "Clorexidina digluconato", "Enjuague bucal", "0.12%", "ODONTOPEDIATRIA"},
            {"Flúor barniz", "Flúoruro de sodio", "Barniz tópico", "5%", "ODONTOPEDIATRIA"},
            {"Flúor gel", "Flúoruro de sodio", "Gel tópico para aplicación", "1.23%", "ODONTOPEDIATRIA"},
            {"Nitrato de plata", "Nitrato de plata", "Solución tópica", "25%", "ODONTOPEDIATRIA"},
            {"Amoxicilina/clavulanato", "Amoxicilina + clavulanato de potasio", "Suspensión oral", "400/57 mg/5 ml", "ODONTOPEDIATRIA"},
            {"Metronidazol", "Metronidazol", "Suspensión oral", "250 mg/5 ml", "ODONTOPEDIATRIA"},
            {"Hidróxido de calcio", "Hidróxido de calcio", "Pasta pulpocida", "Uso profesional", "ODONTOPEDIATRIA"}
        };

        int creados = 0;
        for (String[] f : catalogo) {
            if (medicamentoRepo.findByNombreComercialIgnoreCase(f[0]).isPresent()) {
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
