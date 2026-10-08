package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.*;
import com.clinicahumana.clinica_humana.repository.ExpedienteOdontopediatricoRepository;
import com.clinicahumana.clinica_humana.repository.ExpedientePediatricoRepository;
import com.clinicahumana.clinica_humana.repository.RecetaRepository;
import com.clinicahumana.clinica_humana.service.CitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/recetas")
public class RecetaController {

    @Autowired
    private RecetaRepository recetaRepo;

    @Autowired
    private CitaService citaService;

    @Autowired
    private ExpedientePediatricoRepository expPediatricoRepo;

    @Autowired
    private ExpedienteOdontopediatricoRepository expOdontoRepo;

    // Formulario interactivo para capturar la receta con renglones de medicamentos
    @GetMapping("/nueva/{idCita}")
    public String nuevaReceta(@PathVariable("idCita") Long idCita, Model model, RedirectAttributes redirectAttributes) {
        Cita cita = citaService.buscarPorId(idCita).orElse(null);
        if (cita == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "Cita no encontrada.");
            return "redirect:/citas";
        }

       List<Receta> recetasExistentes = recetaRepo.findByCitaIdCitaOrderByIdRecetaDesc(idCita);
if (!recetasExistentes.isEmpty()) {
    return "redirect:/recetas/imprimir/" + recetasExistentes.get(0).getIdReceta();
}

        Receta receta = new Receta();
        receta.setCita(cita);
        receta.setPaciente(cita.getPaciente());
        receta.setMedico(cita.getMedico());
        receta.setFechaEmision(LocalDate.now());

        // Extraer antecedentes somatométricos si es pediátrico
        if ("PEDIATRIA".equalsIgnoreCase(cita.getEspecialidad())) {
            expPediatricoRepo.findByPacienteIdPacienteOrderByFechaAtencionDesc(cita.getPaciente().getIdPaciente())
                    .stream().findFirst().ifPresent(exp -> {
                        model.addAttribute("peso", exp.getPeso());
                        model.addAttribute("talla", exp.getTalla());
                        model.addAttribute("temperatura", exp.getTemperatura());
                        receta.setDiagnosticoResumen(exp.getDiagnostico());
                        receta.setFirmaBase64(exp.getFirmaMedicoBase64());
                    });
        } else {
            expOdontoRepo.findByPacienteIdPacienteOrderByFechaAtencionDesc(cita.getPaciente().getIdPaciente())
                    .stream().findFirst().ifPresent(exp -> {
                        receta.setDiagnosticoResumen(exp.getDiagnosticoDental());
                        receta.setFirmaBase64(exp.getFirmaMedicoBase64());
                    });
        }

        // Calcular edad exacta del infante
        if (cita.getPaciente().getFechaNacimiento() != null) {
            Period edad = Period.between(cita.getPaciente().getFechaNacimiento(), LocalDate.now());
            model.addAttribute("edadTexto", edad.getYears() + " años " + edad.getMonths() + " meses");
        }

        model.addAttribute("receta", receta);
        return "recetas/captura_receta";
    }

    // Guardar receta con lista dinámica de medicamentos
    @PostMapping("/guardar")
    public String guardarReceta(@ModelAttribute("receta") Receta receta,
                                @RequestParam(name = "medicamento[]", required = false) String[] medicamentos,
                                @RequestParam(name = "presentacion[]", required = false) String[] presentaciones,
                                @RequestParam(name = "dosis[]", required = false) String[] dosis,
                                @RequestParam(name = "frecuencia[]", required = false) String[] frecuencias,
                                @RequestParam(name = "duracion[]", required = false) String[] duraciones,
                                @RequestParam(name = "indicacionesEspecificas[]", required = false) String[] indicaciones,
                                RedirectAttributes redirectAttributes) {

        Cita cita = citaService.buscarPorId(receta.getCita().getIdCita()).orElseThrow();
        receta.setCita(cita);
        receta.setPaciente(cita.getPaciente());
        receta.setMedico(cita.getMedico());

        if (medicamentos != null) {
            for (int i = 0; i < medicamentos.length; i++) {
                if (medicamentos[i] != null && !medicamentos[i].trim().isEmpty()) {
                    RecetaDetalle d = new RecetaDetalle();
                    d.setMedicamento(medicamentos[i].trim());
                    d.setPresentacion(presentaciones != null && presentaciones.length > i ? presentaciones[i] : "");
                    d.setDosis(dosis != null && dosis.length > i ? dosis[i] : "");
                    d.setFrecuencia(frecuencias != null && frecuencias.length > i ? frecuencias[i] : "");
                    d.setDuracion(duraciones != null && duraciones.length > i ? duraciones[i] : "");
                    d.setIndicacionesEspecificas(indicaciones != null && indicaciones.length > i ? indicaciones[i] : "");
                    receta.agregarDetalle(d);
                }
            }
        }

        Receta guardada = recetaRepo.save(receta);
        return "redirect:/recetas/imprimir/" + guardada.getIdReceta();
    }

  // Vista de Impresión con el Formato Membretado Oficial
@GetMapping("/imprimir/{idReceta}")
@Transactional(readOnly = true)
public String imprimirReceta(@PathVariable("idReceta") Long idReceta, Model model) {
    Receta receta = recetaRepo.findById(idReceta).orElseThrow();
    model.addAttribute("receta", receta);

    // Calcular edad
    if (receta.getPaciente().getFechaNacimiento() != null) {
        Period p = Period.between(receta.getPaciente().getFechaNacimiento(), receta.getFechaEmision());
        model.addAttribute("edadCalculada", p.getYears() + " años, " + p.getMonths() + " m");
    }

    // Buscar somatometría para la receta pediátrica
    if ("PEDIATRIA".equalsIgnoreCase(receta.getCita().getEspecialidad())) {
        expPediatricoRepo.findByPacienteIdPacienteOrderByFechaAtencionDesc(receta.getPaciente().getIdPaciente())
                .stream().findFirst().ifPresent(exp -> {
                    model.addAttribute("peso", exp.getPeso());
                    model.addAttribute("talla", exp.getTalla());
                    model.addAttribute("temperatura", exp.getTemperatura());
                });
        return "recetas/receta_pediatrica_print";
    } else {
        return "recetas/receta_odontopediatrica_print";
    }
}
}