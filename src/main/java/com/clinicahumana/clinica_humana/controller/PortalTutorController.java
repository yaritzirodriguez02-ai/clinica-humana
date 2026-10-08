package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Tutor;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.PacienteRepository;
import com.clinicahumana.clinica_humana.repository.TutorRepository;
import com.clinicahumana.clinica_humana.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            RedirectAttributes redirectAttributes) {

        try {
            // 1. Buscar o Registrar Tutor
            Tutor tutor = tutorRepo.findAll().stream()
                    .filter(t -> t.getTelefono() != null && t.getTelefono().equals(telefonoTutor))
                    .findFirst()
                    .orElseGet(() -> {
                        Tutor nuevo = new Tutor();
                        nuevo.setNombre(nombreTutor.trim());
                        nuevo.setApellidos(apellidosTutor.trim());
                        nuevo.setTelefono(telefonoTutor.trim());
                        nuevo.setParentesco(parentesco.trim());
                        return tutorRepo.save(nuevo);
                    });

            // 2. Buscar o Registrar Paciente por CURP
            Optional<Paciente> optPaciente = pacienteRepo.findByCurp(curp.trim().toUpperCase());
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
                redirectAttributes.addFlashAttribute("mensajeError", "El horario seleccionado (" + horaCita + " del " + fechaCita + ") ya se encuentra ocupado con la especialista. Por favor elige otro horario.");
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
            citaRepo.save(cita);

            redirectAttributes.addFlashAttribute("mensajeExito", "¡Cita agendada con éxito para " + paciente.getNombre() + "! Especialista asignada: " + medico.getNombreCompleto() + " el día " + fechaCita + " a las " + horaCita + " hrs.");
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
}