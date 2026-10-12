package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.*;
import com.clinicahumana.clinica_humana.repository.ExpedienteOdontopediatricoRepository;
import com.clinicahumana.clinica_humana.repository.ExpedientePediatricoRepository;
import com.clinicahumana.clinica_humana.repository.MedicamentoRepository;
import com.clinicahumana.clinica_humana.repository.RecetaRepository;
import com.clinicahumana.clinica_humana.service.CitaService;
import com.clinicahumana.clinica_humana.validation.ValidacionesClinicas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private MedicamentoRepository medicamentoRepo;

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

        // Catálogo de fármacos para el autocompletado (según especialidad de la cita)
        cargarCatalogo(model, cita);

        model.addAttribute("receta", receta);
        return "recetas/captura_receta";
    }

    // Carga el datalist del catálogo según la especialidad de la cita
    private void cargarCatalogo(Model model, Cita cita) {
        String categoriaCatalogo = "PEDIATRIA".equalsIgnoreCase(cita.getEspecialidad()) ? "PEDIATRIA" : "ODONTOPEDIATRIA";
        model.addAttribute("medicamentosCatalogo",
                medicamentoRepo.findByCategoriaInOrderByNombreComercialAsc(List.of("GENERAL", categoriaCatalogo)));
    }

    private String valor(String[] arreglo, int i) {
        return (arreglo != null && arreglo.length > i && arreglo[i] != null) ? arreglo[i].trim() : "";
    }

    // Guardar receta con lista dinámica de medicamentos (validación estricta anti basura)
    @PostMapping("/guardar")
    public String guardarReceta(@ModelAttribute("receta") Receta receta,
                                @RequestParam(name = "medicamento[]", required = false) String[] medicamentos,
                                @RequestParam(name = "presentacion[]", required = false) String[] presentaciones,
                                @RequestParam(name = "dosis[]", required = false) String[] dosis,
                                @RequestParam(name = "frecuencia[]", required = false) String[] frecuencias,
                                @RequestParam(name = "duracion[]", required = false) String[] duraciones,
                                @RequestParam(name = "indicacionesEspecificas[]", required = false) String[] indicaciones,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        Cita cita = citaService.buscarPorId(receta.getCita().getIdCita()).orElseThrow();
        receta.setCita(cita);
        receta.setPaciente(cita.getPaciente());
        receta.setMedico(cita.getMedico());

        List<String> errores = new ArrayList<>();
        List<Map<String, String>> renglones = new ArrayList<>();

        // 1. Recomendaciones e indicaciones generales: texto descriptivo real
        String indicacionesGenerales = receta.getIndicacionesGenerales() != null ? receta.getIndicacionesGenerales().trim() : "";
        if (!indicacionesGenerales.isEmpty()) {
            if (!ValidacionesClinicas.recomendacionValida(indicacionesGenerales)) {
                errores.add("Las 'Recomendaciones e Indicaciones Generales' deben ser texto descriptivo real: mínimo 3 palabras coherentes separadas por espacios (sin letras repetidas ni cadenas aleatorias). ");
            } else {
                receta.setIndicacionesGenerales(indicacionesGenerales);
            }
        }

        // 2. Renglones de medicamentos: fármaco real + dosis/frecuencia/duración estructuradas
        if (medicamentos == null || medicamentos.length == 0) {
            errores.add("Debe prescribir al menos un medicamento.");
        } else {
            for (int i = 0; i < medicamentos.length; i++) {
                String med = valor(medicamentos, i);
                if (med.isEmpty()) {
                    continue;
                }
                String present = valor(presentaciones, i);
                String dosisValor = valor(dosis, i);
                String frecValor = valor(frecuencias, i);
                String durValor = valor(duraciones, i);
                String prefijo = "Renglón " + (i + 1) + ": ";

                if (!ValidacionesClinicas.medicamentoValido(med)) {
                    errores.add(prefijo + "el medicamento '" + med + "' no es un nombre de fármaco válido. Use el catálogo interno o un nombre real del vademécum (letras, espacios y signos básicos), sin texto aleatorio.");
                }
                if (!ValidacionesClinicas.dosisValida(dosisValor)) {
                    errores.add(prefijo + "la dosis '" + dosisValor + "' no tiene un formato coherente. Use un número seguido de la unidad (ej. 5 ml, 10 mg, 2 gotas).");
                }
                if (!ValidacionesClinicas.frecuenciaValida(frecValor)) {
                    errores.add(prefijo + "la frecuencia '" + frecValor + "' no tiene un formato coherente. Use por ejemplo: cada 8 horas, 8 hrs o c/8h.");
                }
                if (!ValidacionesClinicas.duracionValida(durValor)) {
                    errores.add(prefijo + "la duración '" + durValor + "' no tiene un formato coherente. Use por ejemplo: por 5 días.");
                }

                Map<String, String> renglon = new LinkedHashMap<>();
                renglon.put("medicamento", med);
                renglon.put("presentacion", present);
                renglon.put("dosis", dosisValor);
                renglon.put("frecuencia", frecValor);
                renglon.put("duracion", durValor);
                renglones.add(renglon);
            }
            if (renglones.isEmpty() && errores.isEmpty()) {
                errores.add("Debe prescribir al menos un medicamento con nombre.");
            }
        }

        // 3. Si hay errores, NO se guarda nada: se regresa el formulario con los mensajes y los renglones capturados
        if (!errores.isEmpty()) {
            model.addAttribute("errorValidacion", errores);
            model.addAttribute("renglones", renglones);
            cargarCatalogo(model, cita);
            return "recetas/captura_receta";
        }

        if (medicamentos != null) {
            for (int i = 0; i < medicamentos.length; i++) {
                if (medicamentos[i] != null && !medicamentos[i].trim().isEmpty()) {
                    RecetaDetalle d = new RecetaDetalle();
                    d.setMedicamento(medicamentos[i].trim());
                    d.setPresentacion(presentaciones != null && presentaciones.length > i ? presentaciones[i] : "");
                    d.setDosis(dosis != null && dosis.length > i ? dosis[i].trim() : "");
                    d.setFrecuencia(frecuencias != null && frecuencias.length > i ? frecuencias[i].trim() : "");
                    d.setDuracion(duraciones != null && duraciones.length > i ? duraciones[i].trim() : "");
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