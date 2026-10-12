package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Medicamento;
import com.clinicahumana.clinica_humana.repository.MedicamentoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * CRUD del catálogo de medicamentos (Vademécum interno de la clínica).
 * Acceso: PEDIATRA, ODONTOPEDIATRA y ADMIN (restringido en SecurityConfig).
 */
@Controller
@RequestMapping("/medicamentos")
public class MedicamentoController {

    @Autowired
    private MedicamentoRepository medicamentoRepo;

    // Listado con búsqueda por nombre
    @GetMapping
    public String listar(@RequestParam(name = "buscar", required = false) String buscar, Model model) {
        List<Medicamento> lista;
        if (buscar != null && !buscar.trim().isEmpty()) {
            lista = medicamentoRepo.findByNombreComercialContainingIgnoreCaseOrderByNombreComercialAsc(buscar.trim());
        } else {
            lista = medicamentoRepo.findAllByOrderByNombreComercialAsc();
        }
        model.addAttribute("medicamentos", lista);
        model.addAttribute("buscar", buscar);
        return "medicamentos/lista";
    }

    // Formulario de alta
    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("medicamento", new Medicamento());
        return "medicamentos/formulario";
    }

    // Formulario de edición
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return medicamentoRepo.findById(id).map(medicamento -> {
            model.addAttribute("medicamento", medicamento);
            return "medicamentos/formulario";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("mensajeError", "El medicamento solicitado no existe.");
            return "redirect:/medicamentos";
        });
    }

    // Guardar alta o edición (con validación de duplicados)
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("medicamento") Medicamento medicamento,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {
        medicamento.setNombreComercial(medicamento.getNombreComercial() != null
                ? medicamento.getNombreComercial().trim() : null);

        boolean duplicado = medicamentoRepo.findByNombreComercialIgnoreCase(medicamento.getNombreComercial())
                .map(existente -> medicamento.getIdMedicamento() == null
                        || !existente.getIdMedicamento().equals(medicamento.getIdMedicamento()))
                .orElse(false);

        if (duplicado) {
            result.rejectValue("nombreComercial", "duplicate",
                    "Ya existe un medicamento registrado con ese nombre comercial.");
        }

        if (result.hasErrors()) {
            return "medicamentos/formulario";
        }

        medicamentoRepo.save(medicamento);
        redirectAttributes.addFlashAttribute("mensajeExito", "Medicamento guardado en el catálogo exitosamente.");
        return "redirect:/medicamentos";
    }

    // Baja del catálogo (las recetas impresas no se ven afectadas: guardan el texto)
    @PostMapping("/eliminar")
    public String eliminar(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        medicamentoRepo.findById(id).ifPresentOrElse(medicamento -> {
            medicamentoRepo.delete(medicamento);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Medicamento '" + medicamento.getNombreComercial() + "' eliminado del catálogo.");
        }, () -> redirectAttributes.addFlashAttribute("mensajeError", "El medicamento solicitado no existe."));
        return "redirect:/medicamentos";
    }
}
