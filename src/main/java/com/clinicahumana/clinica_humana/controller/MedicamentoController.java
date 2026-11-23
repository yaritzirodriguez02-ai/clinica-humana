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
 * Acceso: PEDIATRA, ODONTOLOGIA y ADMIN (restringido en SecurityConfig).
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

    /**
     * Alta rápida desde la captura de receta: la especialista agrega un medicamento
     * nuevo al vademécum sin salir de la receta que está redactando.
     */
    @PostMapping("/rapido")
    public String guardarRapido(@RequestParam("nombreComercial") String nombreComercial,
                                @RequestParam(name = "sustanciaActiva", required = false) String sustanciaActiva,
                                @RequestParam(name = "presentacion", required = false) String presentacion,
                                @RequestParam(name = "concentracion", required = false) String concentracion,
                                @RequestParam(name = "categoria", required = false, defaultValue = "GENERAL") String categoria,
                                @RequestParam(name = "volver", required = false) String volver,
                                RedirectAttributes redirectAttributes) {
        String destino = (volver != null && volver.startsWith("/recetas/") && !volver.contains("//"))
                ? volver : "/medicamentos";

        String nombre = nombreComercial != null ? nombreComercial.trim() : "";
        if (nombre.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Escribe el nombre comercial del medicamento.");
            return "redirect:" + destino;
        }

        boolean duplicado = medicamentoRepo.findByNombreComercialIgnoreCase(nombre).isPresent();
        if (duplicado) {
            redirectAttributes.addFlashAttribute("mensajeInfo",
                    "El medicamento '" + nombre + "' ya estaba en el vademécum.");
            return "redirect:" + destino;
        }

        Medicamento nuevo = new Medicamento();
        nuevo.setNombreComercial(nombre);
        nuevo.setSustanciaActiva(sustanciaActiva != null ? sustanciaActiva.trim() : null);
        nuevo.setPresentacion(presentacion != null ? presentacion.trim() : null);
        nuevo.setConcentracion(concentracion != null ? concentracion.trim() : null);
        nuevo.setCategoria(categoria != null && !categoria.isBlank() ? categoria.toUpperCase() : "GENERAL");
        medicamentoRepo.save(nuevo);

        redirectAttributes.addFlashAttribute("mensajeExito",
                "Medicamento '" + nombre + "' agregado al vademécum.");
        return "redirect:" + destino;
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
