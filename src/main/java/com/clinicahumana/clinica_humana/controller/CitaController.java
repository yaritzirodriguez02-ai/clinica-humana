package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.service.CitaService;
import com.clinicahumana.clinica_humana.service.PacienteService;
import com.clinicahumana.clinica_humana.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/citas")
public class CitaController {

    @Autowired
    private CitaService citaService;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private UsuarioService usuarioService;

    // Listado general de la agenda de citas filtrado por rol
    @GetMapping
    public String listarCitas(@RequestParam(name = "especialidad", required = false) String especialidad,
                              @RequestParam(name = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                              Principal principal,
                              Model model) {

        List<Cita> lista;
        String username = principal.getName();
        Usuario usuarioConectado = usuarioService.buscarPorUsername(username).orElse(null);
        String rol = usuarioConectado != null ? usuarioConectado.getRol() : "";

        LocalDate hoy = LocalDate.now();

        // 1. PEDIATRA (autónoma): sus propias citas — próximas por defecto, o de una fecha específica
        if ("PEDIATRA".equals(rol)) {
            lista = (fecha != null)
                    ? citaService.listarPorMedicoYFecha(usuarioConectado.getIdUsuario(), fecha)
                    : citaService.listarProximasPorMedico(usuarioConectado.getIdUsuario(), hoy);
        }
        // 2. ODONTOPEDIATRA: sus propias citas — próximas por defecto
        else if ("ODONTOPEDIATRA".equals(rol)) {
            lista = (fecha != null)
                    ? citaService.listarPorMedicoYFecha(usuarioConectado.getIdUsuario(), fecha)
                    : citaService.listarProximasPorMedico(usuarioConectado.getIdUsuario(), hoy);
        }
        // 3. RECEPCIONISTA: exclusivamente agenda de Odontología — próximas por defecto
        else if ("RECEPCIONISTA".equals(rol)) {
            lista = (fecha != null)
                    ? citaService.listarPorEspecialidadYFecha("ODONTOLOGIA", fecha)
                    : citaService.listarProximasPorEspecialidad("ODONTOLOGIA", hoy);
        }
        // 4. ADMIN: vista global — próximas por defecto, con filtros opcionales de fecha y especialidad
        else {
            if (fecha != null && especialidad != null && !especialidad.isEmpty()) {
                lista = citaService.listarPorEspecialidadYFecha(especialidad, fecha);
            } else if (fecha != null) {
                lista = citaService.listarPorFecha(fecha);
            } else if (especialidad != null && !especialidad.isEmpty()) {
                lista = citaService.listarProximasPorEspecialidad(especialidad, hoy);
            } else {
                lista = citaService.listarProximas(hoy);
            }
        }

        model.addAttribute("citas", lista);
        model.addAttribute("usuarioActual", usuarioConectado);
        model.addAttribute("filtroEspecialidad", especialidad);
        model.addAttribute("filtroFecha", fecha);
        model.addAttribute("vistaFutura", fecha == null);
        model.addAttribute("esAdmin", "ADMIN".equals(rol));
        return "citas/agenda";
    }

    // Formulario para agendar nueva cita con restricciones por rol
    @GetMapping("/nueva")
    public String formularioNuevaCita(Principal principal, Model model) {
        String username = principal.getName();
        Usuario usuarioConectado = usuarioService.buscarPorUsername(username).orElse(null);
        String rol = usuarioConectado != null ? usuarioConectado.getRol() : "";

        Cita cita = new Cita();
        List<Usuario> medicosPermitidos;

        // RECEPCIONISTA u Odontopediatría: Solo Odontología
        if ("RECEPCIONISTA".equals(rol) || "ODONTOPEDIATRA".equals(rol)) {
            cita.setEspecialidad("ODONTOLOGIA");
            medicosPermitidos = usuarioService.listarTodos().stream()
                    .filter(u -> "ODONTOPEDIATRA".equals(u.getRol()))
                    .toList();
            if ("ODONTOPEDIATRA".equals(rol)) {
                cita.setMedico(usuarioConectado);
            }
        }
        // PEDIATRA: Solo Pediatría asignada a ella misma
        else if ("PEDIATRA".equals(rol)) {
            cita.setEspecialidad("PEDIATRIA");
            cita.setMedico(usuarioConectado);
            medicosPermitidos = usuarioService.listarTodos().stream()
                    .filter(u -> "PEDIATRA".equals(u.getRol()))
                    .toList();
        }
        // ADMIN: Ambas especialidades disponibles
        else {
            medicosPermitidos = usuarioService.listarTodos().stream()
                    .filter(u -> "PEDIATRA".equals(u.getRol()) || "ODONTOPEDIATRA".equals(u.getRol()))
                    .toList();
        }

        model.addAttribute("cita", cita);
        model.addAttribute("pacientes", pacienteService.listarTodos());
        model.addAttribute("medicos", medicosPermitidos);
        model.addAttribute("rolUsuario", rol);

        return "citas/formulario";
    }
// Guardar la cita con validaciones clínicas estrictas
    @PostMapping("/guardar")
    public String guardarCita(@Valid @ModelAttribute("cita") Cita cita,
                              BindingResult result,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : "";
        Usuario usuarioConectado = usuarioService.buscarPorUsername(username).orElse(null);
        String rol = usuarioConectado != null ? usuarioConectado.getRol() : "";

        // Asignación de seguridad según el rol
        if ("RECEPCIONISTA".equals(rol) || "ODONTOPEDIATRA".equals(rol)) {
            cita.setEspecialidad("ODONTOLOGIA");
            if (cita.getMedico() == null || cita.getMedico().getIdUsuario() == null) {
                cita.setMedico(usuarioConectado);
            }
        } else if ("PEDIATRA".equals(rol)) {
            cita.setEspecialidad("PEDIATRIA");
            if (cita.getMedico() == null || cita.getMedico().getIdUsuario() == null) {
                cita.setMedico(usuarioConectado);
            }
        }

        Runnable recargarCombos = () -> {
            model.addAttribute("pacientes", pacienteService.listarTodos());
            List<Usuario> medicos;
            if ("RECEPCIONISTA".equals(rol) || "ODONTOPEDIATRA".equals(rol)) {
                medicos = usuarioService.listarTodos().stream()
                        .filter(u -> "ODONTOPEDIATRA".equals(u.getRol()))
                        .toList();
            } else if ("PEDIATRA".equals(rol)) {
                medicos = usuarioService.listarTodos().stream()
                        .filter(u -> "PEDIATRA".equals(u.getRol()))
                        .toList();
            } else {
                medicos = usuarioService.listarTodos().stream()
                        .filter(u -> "PEDIATRA".equals(u.getRol()) || "ODONTOPEDIATRA".equals(u.getRol()))
                        .toList();
            }
            model.addAttribute("medicos", medicos);
            model.addAttribute("rolUsuario", rol);
        };

        // Si hay errores de validación de Bean Validation (campos obligatorios)
        if (result.hasErrors()) {
            recargarCombos.run();
            // Tomamos el primer mensaje de error para que la alerta roja diga exactamente qué falló
            String primerError = result.getAllErrors().get(0).getDefaultMessage();
            model.addAttribute("errorHorario", primerError != null ? primerError : "Por favor, complete todos los campos obligatorios.");
            return "citas/formulario";
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaCita = cita.getFecha();
        LocalTime horaCita = cita.getHora();
        String motivo = cita.getMotivo() != null ? cita.getMotivo().trim() : "";

        // 1. REGLA: No permitir fechas pasadas
        if (fechaCita == null || fechaCita.isBefore(hoy)) {
            recargarCombos.run();
            model.addAttribute("errorHorario", "Fecha inválida: Las consultas deben programarse a partir de hoy (" + hoy + ") en adelante.");
            return "citas/formulario";
        }

        // 2. REGLA: Horario clínico oficial (09:00 a 19:00 hrs)
        LocalTime apertura = LocalTime.of(9, 0);
        LocalTime cierre = LocalTime.of(19, 0);

        if (horaCita == null || horaCita.isBefore(apertura) || horaCita.isAfter(cierre)) {
            recargarCombos.run();
            model.addAttribute("errorHorario", "Horario no permitido: La atención clínica es de 09:00 a 19:00 hrs.");
            return "citas/formulario";
        }

        // 3. REGLA: Motivo clínico coherente
        if (motivo.length() < 10) {
            recargarCombos.run();
            model.addAttribute("errorHorario", "Motivo no válido: Especifique una descripción clínica de al menos 10 caracteres.");
            return "citas/formulario";
        }

        String[] palabras = motivo.split("\\s+");
        boolean esTextoInvalido = false;

        if (palabras.length < 2) {
            esTextoInvalido = true;
        } else {
            for (String p : palabras) {
                if (p.length() > 22 || p.matches(".*(.)\\1{3,}.*")) {
                    esTextoInvalido = true;
                    break;
                }
            }
        }

        if (esTextoInvalido) {
            recargarCombos.run();
            model.addAttribute("errorHorario", "Motivo no válido: Debe describir una causa clínica real con palabras coherentes (ej. 'Revisión por dolor dental', 'Control de niño sano').");
            return "citas/formulario";
        }

        try {
            citaService.agendarCita(cita);
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Cita agendada exitosamente!");
            return "redirect:/citas";
        } catch (IllegalStateException e) {
            recargarCombos.run();
            model.addAttribute("errorHorario", e.getMessage());
            return "citas/formulario";
        }
    }

    // Cancelar cita con motivo
    @PostMapping("/cancelar")
    public String cancelarCita(@RequestParam("idCita") Long idCita,
                               @RequestParam("motivoCancelacion") String motivoCancelacion,
                               RedirectAttributes redirectAttributes) {
        try {
            citaService.cancelarCita(idCita, motivoCancelacion);
            redirectAttributes.addFlashAttribute("mensajeExito", "Cita cancelada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al cancelar la cita: " + e.getMessage());
        }
        return "redirect:/citas";
    }
    // Marcar Inasistencia (No Asistió)
    @PostMapping("/no-asistio")
    public String marcarNoAsistio(@RequestParam("idCita") Long idCita,
                                  RedirectAttributes redirectAttributes) {
        try {
            // Se marca directamente como NO_ASISTIO (agendarCita forzaría estado PROGRAMADA)
            citaService.marcarNoAsistio(idCita);
            redirectAttributes.addFlashAttribute("mensajeExito", "La cita se registró como 'No Asistió'.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al actualizar la cita: " + e.getMessage());
        }
        return "redirect:/citas";
    }

    // Reprogramar Fecha y Hora
    @PostMapping("/reprogramar")
    public String reprogramarCita(@RequestParam("idCita") Long idCita,
                                  @RequestParam("nuevaFecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nuevaFecha,
                                  @RequestParam("nuevaHora") LocalTime nuevaHora,
                                  RedirectAttributes redirectAttributes) {
        try {
            LocalDate hoy = LocalDate.now();
            if (nuevaFecha.isBefore(hoy)) {
                redirectAttributes.addFlashAttribute("mensajeError", "La fecha para reagendar debe ser a partir de hoy.");
                return "redirect:/citas";
            }
            LocalTime apertura = LocalTime.of(9, 0);
            LocalTime cierre = LocalTime.of(19, 0);
            if (nuevaHora.isBefore(apertura) || nuevaHora.isAfter(cierre)) {
                redirectAttributes.addFlashAttribute("mensajeError", "El horario clínico permitido es de 09:00 a 19:00 hrs.");
                return "redirect:/citas";
            }

            Cita cita = citaService.buscarPorId(idCita).orElse(null);
            if (cita != null) {
                cita.setFecha(nuevaFecha);
                cita.setHora(nuevaHora);
                cita.setEstado("PROGRAMADA");
                citaService.agendarCita(cita);
                redirectAttributes.addFlashAttribute("mensajeExito", "Cita reagendada con éxito para el " + nuevaFecha + " a las " + nuevaHora + " hrs.");
            }
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al reagendar la cita: " + e.getMessage());
        }
        return "redirect:/citas";
    }
}