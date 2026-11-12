package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.AreaClinica;
import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.Empleado;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Tutor;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.EmpleadoRepository;
import com.clinicahumana.clinica_humana.repository.PacienteRepository;
import com.clinicahumana.clinica_humana.repository.TutorRepository;
import com.clinicahumana.clinica_humana.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Controller
@RequestMapping("/portal-citas")
public class PortalTutorController {

    @Autowired
    private PacienteRepository pacienteRepo;

    @Autowired
    private TutorRepository tutorRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private CitaRepository citaRepo;

    @Autowired
    private EmpleadoRepository empleadoRepo;

    private static final String WHATSAPP_CLINICA = "7474729051";

    // Pantalla de solicitud de cita para el tutor
    @GetMapping
    public String formularioSolicitud(Model model) {
        return "tutor/solicitar_cita";
    }

    // Procesar la solicitud
    @PostMapping("/solicitar")
    public String procesarSolicitud(
            @RequestParam("curp") String curp,
            @RequestParam("nombrePaciente") String nombrePaciente,
            @RequestParam("apellidosPaciente") String apellidosPaciente,
            @RequestParam("fechaNacimiento") String fechaNacimiento,
            @RequestParam("sexo") String sexo,
            @RequestParam("nombreTutor") String nombreTutor,
            @RequestParam("apellidosTutor") String apellidosTutor,
            @RequestParam("telefonoTutor") String telefonoTutor,
            @RequestParam("parentesco") String parentesco,
            @RequestParam("especialidad") String especialidad,
            @RequestParam("fechaCita") String fechaCita,
            @RequestParam("horaCita") String horaCita,
            @RequestParam("motivo") String motivo,
            @RequestParam(value = "aceptaAvisoFotos", required = false) String aceptaAvisoFotos,
            RedirectAttributes redirectAttributes) {

        try {
            // La solicitud cae en el área de la especialidad elegida: los paneles no se mezclan
            AreaClinica area = AreaClinica.paraEspecialidad(especialidad);
            if (area == null) {
                throw new IllegalArgumentException("Debe seleccionar una especialidad válida (Pediatría u Odontología).");
            }
            boolean aceptoFotos = "on".equalsIgnoreCase(aceptaAvisoFotos)
                    || "true".equalsIgnoreCase(aceptaAvisoFotos)
                    || "1".equals(aceptaAvisoFotos);
            if (area == AreaClinica.ODONTOLOGIA && !aceptoFotos) {
                throw new IllegalArgumentException("Para la atención de Odontología debe aceptar el aviso de privacidad y el uso de imágenes clínicas.");
            }

            // 1. Buscar o Registrar Tutor dentro del área
            Tutor tutor = tutorRepo.findByTelefonoAndArea(telefonoTutor.trim(), area).stream()
                    .findFirst()
                    .orElseGet(() -> {
                        Tutor nuevo = new Tutor();
                        nuevo.setNombre(nombreTutor.trim());
                        nuevo.setApellidos(apellidosTutor.trim());
                        nuevo.setTelefono(telefonoTutor.trim());
                        nuevo.setParentesco(parentesco.trim());
                        nuevo.setArea(area);
                        return tutorRepo.save(nuevo);
                    });

            // 2. Buscar o Registrar Paciente por CURP dentro del área
            Optional<Paciente> optPaciente = pacienteRepo.findByCurpAndArea(curp.trim().toUpperCase(), area);
            Paciente paciente;
            if (optPaciente.isPresent()) {
                paciente = optPaciente.get();
            } else {
                paciente = new Paciente();
                paciente.setCurp(curp.trim().toUpperCase());
                paciente.setNombre(nombrePaciente.trim());
                paciente.setApellidos(apellidosPaciente.trim());
                if (fechaNacimiento != null && !fechaNacimiento.isEmpty()) {
                    paciente.setFechaNacimiento(LocalDate.parse(fechaNacimiento));
                }
               String sexoNormalizado = (sexo != null && !sexo.isEmpty()) ? String.valueOf(sexo.trim().toUpperCase().charAt(0)) : "M";
paciente.setSexo(sexoNormalizado);
                paciente.setTutor(tutor);
                paciente.setArea(area);
                paciente = pacienteRepo.save(paciente);
            }

            // 3. Asignar Médico Titular según especialidad
            String usernameMedico = "PEDIATRIA".equalsIgnoreCase(especialidad) ? "pilar.rios" : "gloria.gonzalez";
            Usuario medico = usuarioRepo.findByUsername(usernameMedico)
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró especialista para " + especialidad));

            // 4. Validar que no haya empalme en la misma fecha y hora para el médico
            LocalDate f = LocalDate.parse(fechaCita);
            LocalTime h = LocalTime.parse(horaCita);
            boolean ocupado = citaRepo.findByMedicoIdUsuarioAndFecha(medico.getIdUsuario(), f)
                    .stream()
                    .anyMatch(c -> c.getHora().equals(h) && !"CANCELADA".equalsIgnoreCase(c.getEstado()));

            if (ocupado) {
                redirectAttributes.addFlashAttribute("mensajeError", "El horario seleccionado ya se encuentra ocupado para este médico. Por favor, elija otra hora o fecha.");
                return "redirect:/portal-citas";
            }

            // 5. Crear la Cita
            Cita cita = new Cita();
            cita.setPaciente(paciente);
            cita.setMedico(medico);
            cita.setEspecialidad(especialidad.toUpperCase());
            cita.setFecha(f);
            cita.setHora(h);
            cita.setMotivo(motivo);
            cita.setEstado("PROGRAMADA");
            cita.setAceptaUsoFotosClinicas(aceptoFotos);
            citaRepo.save(cita);

            String telefonoMedico = empleadoRepo.findByUsuarioIdUsuario(medico.getIdUsuario())
                    .map(Empleado::getTelefono)
                    .map(t -> t.replaceAll("[^0-9]", ""))
                    .filter(t -> t.length() >= 10)
                    .orElse("");
            String telefonoContacto = telefonoMedico.isEmpty() ? WHATSAPP_CLINICA : telefonoMedico;
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Cita agendada con éxito para " + paciente.getNombre() + "! Especialista asignada: " + medico.getNombreCompleto() + " el día " + fechaCita + " a las " + horaCita + " hrs.");
            redirectAttributes.addFlashAttribute("whatsappCita", whatsappUrl(telefonoContacto,
                    "Hola, agendé una cita en Clínica Humana.\n"
                            + "Folio: #" + cita.getIdCita() + "\n"
                            + "Paciente: " + paciente.getNombre() + " " + paciente.getApellidos() + "\n"
                            + "Especialidad: " + especialidad.toUpperCase() + "\n"
                            + "Especialista: " + medico.getNombreCompleto() + "\n"
                            + "Fecha: " + fechaCita + " a las " + horaCita + " hrs.\n"
                            + "Motivo: " + motivo));
            redirectAttributes.addFlashAttribute("whatsappTelefono", telefonoContacto);
            redirectAttributes.addFlashAttribute("whatsappEsClinica", telefonoMedico.isEmpty());
            return "redirect:/portal-citas/confirmacion";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al agendar la cita: " + e.getMessage());
            return "redirect:/portal-citas";
        }
    }

    @GetMapping("/confirmacion")
    public String confirmacion() {
        return "tutor/confirmacion_cita";
    }

    /** Enlace directo de WhatsApp con el número de la especialista (lada 52 para México). */
    private String whatsappUrl(String telefono, String texto) {
        String digitos = telefono == null ? "" : telefono.replaceAll("[^0-9]", "");
        if (digitos.length() == 10) {
            digitos = "52" + digitos;
        }
        return "https://wa.me/" + digitos + "?text=" + URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }
}