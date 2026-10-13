package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.ExpedienteOdontopediatrico;
import com.clinicahumana.clinica_humana.model.ExpedientePediatrico;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Tutor;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.service.ClinicoYPagosService;
import com.clinicahumana.clinica_humana.service.PacienteService;
import com.clinicahumana.clinica_humana.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private ClinicoYPagosService clinicoService;

    @Autowired
    private UsuarioService usuarioService;

    // Listar todos los pacientes con opción de búsqueda (RF02)
    @GetMapping
    public String listarPacientes(@RequestParam(name = "buscar", required = false) String buscar, Model model) {
        List<Paciente> lista;
        if (buscar != null && !buscar.trim().isEmpty()) {
            lista = pacienteService.buscarPorNombreOApellido(buscar.trim());
        } else {
            lista = pacienteService.listarTodos();
        }
        model.addAttribute("pacientes", lista);
        model.addAttribute("buscar", buscar);
        return "pacientes/lista_pacientes";
    }

    // Historial clínico completo del paciente (pediátrico + odontopediátrico) ordenado cronológicamente
    @GetMapping("/expediente/{idPaciente}")
    public String verExpedientePaciente(@PathVariable("idPaciente") Long idPaciente,
                                        Principal principal,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (!pacienteService.buscarPorId(idPaciente).isPresent()) {
            redirectAttributes.addFlashAttribute("mensajeError", "El paciente solicitado no existe.");
            return "redirect:/pacientes";
        }

        Paciente paciente = pacienteService.buscarPorId(idPaciente).orElseThrow();
        model.addAttribute("paciente", paciente);

        Usuario usuarioActual = usuarioService.buscarPorUsername(principal != null ? principal.getName() : "").orElse(null);
        String rol = usuarioActual != null ? usuarioActual.getRol() : "";
        boolean esAdmin = "ADMIN".equals(rol);
        boolean esPediatra = "PEDIATRA".equals(rol);
        boolean esOdontopediatra = "ODONTOPEDIATRA".equals(rol);
        boolean esRecepcionista = "RECEPCIONISTA".equals(rol);

        model.addAttribute("rolUsuario", rol);
        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("esPediatra", esPediatra);
        model.addAttribute("esOdontopediatra", esOdontopediatra);
        model.addAttribute("esRecepcionista", esRecepcionista);
        model.addAttribute("idPaciente", idPaciente);

        List<ExpedientePediatrico> historiaPediatrica;
        List<ExpedienteOdontopediatrico> historiaOdontopediatrica;

        if (esPediatra) {
            historiaPediatrica = clinicoService.obtenerHistorialPediatricoPorMedico(idPaciente, usuarioActual.getIdUsuario());
            historiaOdontopediatrica = List.of();
        } else if (esOdontopediatra || esRecepcionista) {
            historiaPediatrica = List.of();
            historiaOdontopediatrica = clinicoService.obtenerHistorialOdontopediatricoPorMedico(idPaciente, usuarioActual.getIdUsuario());
        } else {
            historiaPediatrica = clinicoService.obtenerHistorialPediatrico(idPaciente);
            historiaOdontopediatrica = clinicoService.obtenerHistorialOdontopediatrico(idPaciente);
        }

        List<ExpedientePediatrico> pediatraCronologica = new ArrayList<>(historiaPediatrica);
        Collections.reverse(pediatraCronologica);
        List<ExpedienteOdontopediatrico> odontoCronologica = new ArrayList<>(historiaOdontopediatrica);
        Collections.reverse(odontoCronologica);

        model.addAttribute("historiaPediatrica", historiaPediatrica);
        model.addAttribute("historiaOdontopediatrica", historiaOdontopediatrica);
        model.addAttribute("historiaPediatricaCronologica", pediatraCronologica);
        model.addAttribute("historiaOdontopediatricaCronologica", odontoCronologica);

        return "pacientes/ver_expediente";
    }

    // Mostrar formulario de nuevo paciente (RF01, RF03)
    @GetMapping("/nuevo")
    public String formularioNuevoPaciente(Model model) {
        Paciente paciente = new Paciente();
        paciente.setTutor(new Tutor()); // Instanciar para el formulario anidado
        model.addAttribute("paciente", paciente);
        model.addAttribute("tutoresExistentes", pacienteService.listarTutores());
        return "pacientes/formulario";
    }

    // Guardar nuevo paciente o editar existente
    @PostMapping("/guardar")
    public String guardarPaciente(@Valid @ModelAttribute("paciente") Paciente paciente,
                                 BindingResult result,
                                 @RequestParam(name = "tieneAlergias", required = false) String tieneAlergias,
                                 @RequestParam(name = "tieneAntecedentes", required = false) String tieneAntecedentes,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        // Condicionales Sí/No del formulario: si la respuesta es "No", se limpia el detalle
        if (!"S".equals(tieneAlergias)) {
            paciente.setAlergias("");
        } else if (paciente.getAlergias() != null) {
            paciente.setAlergias(paciente.getAlergias().trim());
        }

        if (!"S".equals(tieneAntecedentes)) {
            paciente.setAntecedentes("");
        } else if (paciente.getAntecedentes() != null) {
            paciente.setAntecedentes(paciente.getAntecedentes().trim());
        }

        if (result.hasErrors()) {
            model.addAttribute("tutoresExistentes", pacienteService.listarTutores());
            return "pacientes/formulario";
        }

        try {
            pacienteService.registrarOActualizarPaciente(paciente);
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Paciente y tutor guardados exitosamente!");
            return "redirect:/pacientes";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorCurp", e.getMessage());
            model.addAttribute("tutoresExistentes", pacienteService.listarTutores());
            return "pacientes/formulario";
        }
    }

    // Editar paciente existente
    @GetMapping("/editar/{id}")
    public String editarPaciente(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return pacienteService.buscarPorId(id).map(paciente -> {
            model.addAttribute("paciente", paciente);
            model.addAttribute("tutoresExistentes", pacienteService.listarTutores());
            return "pacientes/formulario";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("mensajeError", "El paciente con ID " + id + " no existe.");
            return "redirect:/pacientes";
        });
    }
}