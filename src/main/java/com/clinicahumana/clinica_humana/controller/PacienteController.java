package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Tutor;
import com.clinicahumana.clinica_humana.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    @Autowired
    private PacienteService pacienteService;

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
        return "pacientes/lista";
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
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
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