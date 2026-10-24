package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Tutor;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.PacienteRepository;
import com.clinicahumana.clinica_humana.repository.TutorRepository;
import com.clinicahumana.clinica_humana.service.CitaService;
import com.clinicahumana.clinica_humana.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Portal Público de Citas para Tutores (agendamiento en línea SIN sesión).
 * Rutas bajo /public/** permitidas en SecurityConfig.
 * La cita se guarda en la misma base de datos general con estado PROGRAMADA,
 * por lo que aparece de inmediato en la agenda interna de las doctoras y en recepción.
 */
@Controller
@RequestMapping("/public")
public class PortalPublicoController {

    // Mismos patrones de negocio que usan los formularios internos
    private static final Pattern PATRON_NOMBRES = Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,100}$");
    private static final Pattern PATRON_CURP = Pattern.compile("^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9A-Z]{2}$");
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^[0-9]{10}$");
    private static final Pattern PATRON_EMAIL = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private CitaService citaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PacienteRepository pacienteRepo;

    @Autowired
    private TutorRepository tutorRepo;

    // Pantalla pública del portal (sin sesión)
    @GetMapping("/agendar-cita")
    public String formulario(Model model) {
        model.addAttribute("hoy", LocalDate.now().toString());
        return "public/agendar_cita";
    }

    /**
     * Endpoint JSON público: médicos activos filtrados por especialidad elegida.
     * Solo expone id y nombre (nunca credenciales ni datos internos del Usuario).
     */
    @GetMapping("/medicos")
    @ResponseBody
    public List<Map<String, Object>> medicosPorEspecialidad(@RequestParam("especialidad") String especialidad) {
        String rol = "PEDIATRIA".equalsIgnoreCase(especialidad) ? "PEDIATRA"
                : "ODONTOLOGIA".equalsIgnoreCase(especialidad) ? "ODONTOPEDIATRA" : "";

        if (rol.isEmpty()) {
            return List.of();
        }

        return usuarioService.listarMedicosPorEspecialidad(rol).stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .map(m -> Map.<String, Object>of(
                        "id", m.getIdUsuario(),
                        "nombre", m.getNombreCompleto()))
                .toList();
    }

    /**
     * Endpoint JSON público: horas ya ocupadas del médico en la fecha elegida,
     * para deshabilitarlas en el selector antes de que el tutor confirme.
     */
    @GetMapping("/horarios-ocupados")
    @ResponseBody
    public List<String> horariosOcupados(@RequestParam("medico") Long idMedico,
                                         @RequestParam("fecha") String fecha) {
        try {
            LocalDate f = LocalDate.parse(fecha);
            return citaService.listarPorMedicoYFecha(idMedico, f).stream()
                    .filter(c -> !"CANCELADA".equalsIgnoreCase(c.getEstado()))
                    .map(c -> c.getHora().toString())
                    .toList();
        } catch (DateTimeParseException e) {
            return List.of();
        }
    }

    // Procesamiento del formulario público
    @PostMapping("/agendar-cita")
    public String agendarCitaPublica(
            @RequestParam("nombrePaciente") String nombrePaciente,
            @RequestParam("apellidosPaciente") String apellidosPaciente,
            @RequestParam("curp") String curp,
            @RequestParam("fechaNacimiento") String fechaNacimiento,
            @RequestParam("sexo") String sexo,
            @RequestParam("nombreTutor") String nombreTutor,
            @RequestParam("apellidosTutor") String apellidosTutor,
            @RequestParam("parentesco") String parentesco,
            @RequestParam("telefonoTutor") String telefonoTutor,
            @RequestParam("emailTutor") String emailTutor,
            @RequestParam("especialidad") String especialidad,
            @RequestParam("idMedico") Long idMedico,
            @RequestParam("fechaCita") String fechaCita,
            @RequestParam("horaCita") String horaCita,
            @RequestParam("motivo") String motivo,
            RedirectAttributes redirectAttributes) {

        // ---------- Validaciones de los datos del infante ----------
        String curpNorm = curp == null ? "" : curp.trim().toUpperCase();
        if (!PATRON_NOMBRES.matcher(nombrePaciente.trim()).matches()) {
            return error(redirectAttributes, "Nombre del paciente inválido: solo se permiten letras y espacios.");
        }
        if (!PATRON_NOMBRES.matcher(apellidosPaciente.trim()).matches()) {
            return error(redirectAttributes, "Apellidos del paciente inválidos: solo se permiten letras y espacios.");
        }
        if (!PATRON_CURP.matcher(curpNorm).matches()) {
            return error(redirectAttributes, "La CURP no cumple con el formato estándar oficial de 18 caracteres.");
        }

        LocalDate fNacimiento;
        try {
            fNacimiento = LocalDate.parse(fechaNacimiento);
            if (fNacimiento.isAfter(LocalDate.now())) {
                return error(redirectAttributes, "La fecha de nacimiento no puede ser futura.");
            }
        } catch (DateTimeParseException e) {
            return error(redirectAttributes, "Fecha de nacimiento inválida.");
        }

        String sexoNorm = (sexo != null && !sexo.isEmpty()) ? sexo.trim().toUpperCase() : "";
        if (!"M".equals(sexoNorm) && !"F".equals(sexoNorm)) {
            return error(redirectAttributes, "Debe indicar el sexo del paciente (M o F).");
        }

        // ---------- Validaciones del tutor legal ----------
        if (!PATRON_NOMBRES.matcher(nombreTutor.trim()).matches()) {
            return error(redirectAttributes, "Nombre del tutor inválido: solo se permiten letras y espacios.");
        }
        if (!PATRON_NOMBRES.matcher(apellidosTutor.trim()).matches()) {
            return error(redirectAttributes, "Apellidos del tutor inválidos: solo se permiten letras y espacios.");
        }
        String parentescoNorm = parentesco == null ? "" : parentesco.trim();
        if (parentescoNorm.isEmpty() || parentescoNorm.length() > 50) {
            return error(redirectAttributes, "Debe indicar el parentesco con el paciente.");
        }
        String telefonoNorm = telefonoTutor == null ? "" : telefonoTutor.trim();
        if (!PATRON_TELEFONO.matcher(telefonoNorm).matches()) {
            return error(redirectAttributes, "El teléfono del tutor debe contener exactamente 10 dígitos numéricos.");
        }
        String emailNorm = emailTutor == null ? "" : emailTutor.trim();
        if (!PATRON_EMAIL.matcher(emailNorm).matches()) {
            return error(redirectAttributes, "Debe proporcionar un correo electrónico válido.");
        }

        // ---------- Validaciones de la cita (mismas reglas que el sistema interno) ----------
        String espNorm = especialidad == null ? "" : especialidad.trim().toUpperCase();
        if (!"PEDIATRIA".equals(espNorm) && !"ODONTOLOGIA".equals(espNorm)) {
            return error(redirectAttributes, "Debe seleccionar una especialidad válida (Pediatría u Odontología).");
        }

        String rolEsperado = "PEDIATRIA".equals(espNorm) ? "PEDIATRA" : "ODONTOPEDIATRA";
        Usuario medico = usuarioService.buscarPorId(idMedico == null ? -1L : idMedico).orElse(null);
        if (medico == null || !rolEsperado.equals(medico.getRol()) || !Boolean.TRUE.equals(medico.getActivo())) {
            return error(redirectAttributes, "El médico seleccionado no está disponible para la especialidad elegida. Vuelva a elegirlo.");
        }

        LocalDate fCita;
        try {
            fCita = LocalDate.parse(fechaCita);
        } catch (DateTimeParseException e) {
            return error(redirectAttributes, "Fecha de la cita inválida.");
        }
        LocalDate hoy = LocalDate.now();
        if (fCita.isBefore(hoy)) {
            return error(redirectAttributes, "Fecha inválida: las consultas deben programarse a partir de hoy (" + hoy + ") en adelante.");
        }

        LocalTime hCita;
        try {
            hCita = LocalTime.parse(horaCita);
        } catch (DateTimeParseException e) {
            return error(redirectAttributes, "Horario de la cita inválido.");
        }
        LocalTime apertura = LocalTime.of(9, 0);
        LocalTime cierre = LocalTime.of(19, 0);
        if (hCita.isBefore(apertura) || hCita.isAfter(cierre)) {
            return error(redirectAttributes, "Horario no permitido: la atención clínica es de 09:00 a 19:00 hrs.");
        }

        String motivoNorm = motivo == null ? "" : motivo.trim();
        if (motivoNorm.length() < 10 || motivoNorm.length() > 255) {
            return error(redirectAttributes, "El motivo debe describir la causa de la consulta (entre 10 y 255 caracteres).");
        }
        String[] palabras = motivoNorm.split("\\s+");
        boolean esTextoInvalido = palabras.length < 2;
        if (!esTextoInvalido) {
            for (String p : palabras) {
                if (p.length() > 22 || p.matches(".*(.)\\1{3,}.*")) {
                    esTextoInvalido = true;
                    break;
                }
            }
        }
        if (esTextoInvalido) {
            return error(redirectAttributes, "El motivo no es válido: describa una causa real con palabras coherentes (ej. 'Revisión por dolor dental', 'Control de niño sano').");
        }

        try {
            // 1. Buscar o registrar Tutor (por teléfono; si no existe, se crea)
            Tutor tutor = tutorRepo.findByTelefono(telefonoNorm).stream()
                    .findFirst()
                    .orElseGet(() -> {
                        Tutor nuevo = new Tutor();
                        nuevo.setNombre(nombreTutor.trim());
                        nuevo.setApellidos(apellidosTutor.trim());
                        nuevo.setParentesco(parentescoNorm);
                        nuevo.setTelefono(telefonoNorm);
                        nuevo.setEmail(emailNorm);
                        return tutorRepo.save(nuevo);
                    });

            // 2. Buscar o registrar Paciente por CURP (si ya existe, se reutiliza sin tocar su tutor actual)
            Paciente paciente = pacienteRepo.findByCurp(curpNorm).orElseGet(() -> {
                Paciente nuevo = new Paciente();
                nuevo.setCurp(curpNorm);
                nuevo.setNombre(nombrePaciente.trim());
                nuevo.setApellidos(apellidosPaciente.trim());
                nuevo.setFechaNacimiento(fNacimiento);
                nuevo.setSexo(sexoNorm);
                nuevo.setTutor(tutor);
                return pacienteRepo.save(nuevo);
            });

            // 3. Crear la Cita (agendarCita valida empalme y fuerza estado PROGRAMADA)
            Cita cita = new Cita();
            cita.setPaciente(paciente);
            cita.setMedico(medico);
            cita.setEspecialidad(espNorm);
            cita.setFecha(fCita);
            cita.setHora(hCita);
            cita.setMotivo(motivoNorm);
            Cita citaGuardada = citaService.agendarCita(cita);

            redirectAttributes.addFlashAttribute("mensajeExito",
                    "¡Cita agendada con éxito! Folio #" + citaGuardada.getIdCita()
                            + " para " + paciente.getNombre() + " " + paciente.getApellidos()
                            + " con " + medico.getNombreCompleto() + " el día " + fCita
                            + " a las " + hCita + " hrs. Aparecerá de inmediato en la agenda de la clínica.");
            return "redirect:/public/agendar-cita";

        } catch (IllegalStateException e) {
            // Empalme de horario detectado por CitaService
            return error(redirectAttributes, e.getMessage());
        } catch (Exception e) {
            return error(redirectAttributes, "No fue posible agendar la cita: " + e.getMessage());
        }
    }

    private String error(RedirectAttributes redirectAttributes, String mensaje) {
        redirectAttributes.addFlashAttribute("mensajeError", mensaje);
        return "redirect:/public/agendar-cita";
    }
}
