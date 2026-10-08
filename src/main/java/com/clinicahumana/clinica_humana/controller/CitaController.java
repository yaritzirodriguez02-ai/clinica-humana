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

    // Listado general de la agenda de citas
    @GetMapping
    public String listarCitas(@RequestParam(name = "especialidad", required = false) String especialidad,
                              @RequestParam(name = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                              java.security.Principal principal,
                              Model model) {
        
        List<Cita> lista;
        String username = principal.getName();
        Usuario usuarioConectado = usuarioService.buscarPorUsername(username).orElse(null);

        // Si es PEDIATRA, filtra solo las citas de la Dra. Pilar
        if (usuarioConectado != null && "PEDIATRA".equals(usuarioConectado.getRol())) {
            lista = citaService.listarPorMedicoYFecha(usuarioConectado.getIdUsuario(), fecha != null ? fecha : LocalDate.now());
            if (lista.isEmpty() && fecha == null) {
                lista = citaService.listarTodas().stream()
                        .filter(c -> c.getMedico().getIdUsuario().equals(usuarioConectado.getIdUsuario()))
                        .toList();
            }
        } 
        // Si es ODONTOPEDIATRA, filtra solo las citas de la Dra. Gloria
        else if (usuarioConectado != null && "ODONTOPEDIATRA".equals(usuarioConectado.getRol())) {
            lista = citaService.listarPorMedicoYFecha(usuarioConectado.getIdUsuario(), fecha != null ? fecha : LocalDate.now());
            if (lista.isEmpty() && fecha == null) {
                lista = citaService.listarTodas().stream()
                        .filter(c -> c.getMedico().getIdUsuario().equals(usuarioConectado.getIdUsuario()))
                        .toList();
            }
        } 
        // Si es RECEPCIONISTA o ADMIN, ve toda la agenda
        else {
            if (especialidad != null && !especialidad.isEmpty() && fecha != null) {
                lista = citaService.listarPorEspecialidadYFecha(especialidad, fecha);
            } else {
                lista = citaService.listarTodas();
            }
        }

        model.addAttribute("citas", lista);
        model.addAttribute("usuarioActual", usuarioConectado);
        model.addAttribute("filtroEspecialidad", especialidad);
        model.addAttribute("filtroFecha", fecha);
        return "citas/agenda";
    }

    // Formulario para agendar nueva cita
    @GetMapping("/nueva")
    public String formularioNuevaCita(Model model) {
        model.addAttribute("cita", new Cita());
        model.addAttribute("pacientes", pacienteService.listarTodos());
        
        // Filtrar únicamente los especialistas médicos (PEDIATRA y ODONTOPEDIATRA)
        List<Usuario> soloMedicos = usuarioService.listarTodos().stream()
                .filter(u -> "PEDIATRA".equals(u.getRol()) || "ODONTOPEDIATRA".equals(u.getRol()))
                .toList();
        model.addAttribute("medicos", soloMedicos);
        
        return "citas/formulario";
    }

    // Guardar la cita con validaciones de fecha, horario clínico y motivo
    @PostMapping("/guardar")
    public String guardarCita(@Valid @ModelAttribute("cita") Cita cita,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        // Método auxiliar para recargar combos si hay error
        Runnable recargarCombos = () -> {
            model.addAttribute("pacientes", pacienteService.listarTodos());
            model.addAttribute("medicos", usuarioService.listarTodos().stream()
                    .filter(u -> "PEDIATRA".equals(u.getRol()) || "ODONTOPEDIATRA".equals(u.getRol()))
                    .toList());
        };

        if (result.hasErrors()) {
            recargarCombos.run();
            return "citas/formulario";
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaCita = cita.getFecha();
        LocalTime horaCita = cita.getHora();
        String motivo = cita.getMotivo() != null ? cita.getMotivo().trim() : "";

        // 1. REGLA DE NEGOCIO: La fecha no puede ser anterior al día actual ni fechas irreales
        if (fechaCita == null || fechaCita.isBefore(hoy)) {
            recargarCombos.run();
            model.addAttribute("errorHorario", "Fecha inválida: Las consultas deben programarse a partir de hoy (" + hoy + ") en adelante.");
            return "citas/formulario";
        }

        // 2. REGLA DE NEGOCIO: Horario de atención clínica (09:00 a 19:00 hrs)
        LocalTime apertura = LocalTime.of(9, 0);
        LocalTime cierre = LocalTime.of(19, 0);

        if (horaCita == null || horaCita.isBefore(apertura) || horaCita.isAfter(cierre)) {
            recargarCombos.run();
            model.addAttribute("errorHorario", "Horario no permitido: La atención médica es exclusivamente de 09:00 a 19:00 hrs.");
            return "citas/formulario";
        }

        // 3. REGLA DE NEGOCIO: Motivo médico descriptivo (mínimo 10 caracteres)
        if (motivo.length() < 10) {
            recargarCombos.run();
            model.addAttribute("errorHorario", "Motivo no válido: Especifique una descripción clínica real de al menos 10 caracteres.");
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

    // Cancelar cita
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
}