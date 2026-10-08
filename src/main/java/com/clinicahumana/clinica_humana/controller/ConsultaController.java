package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.ExpedienteOdontopediatrico;
import com.clinicahumana.clinica_humana.model.ExpedientePediatrico;
import com.clinicahumana.clinica_humana.service.CitaService;
import com.clinicahumana.clinica_humana.service.ClinicoYPagosService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/consultas")
public class ConsultaController {

    @Autowired
    private CitaService citaService;

    @Autowired
    private ClinicoYPagosService clinicoService;

    // Atender Cita (redirige a Pediatría u Odontopediatría según la especialidad)
    @GetMapping("/atender/{idCita}")
    public String atenderCita(@PathVariable("idCita") Long idCita, Model model, RedirectAttributes redirectAttributes) {
        Cita cita = citaService.buscarPorId(idCita).orElse(null);

        if (cita == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "La cita no existe.");
            return "redirect:/citas";
        }

        if ("COMPLETADA".equals(cita.getEstado())) {
            redirectAttributes.addFlashAttribute("mensajeError", "Esta cita ya fue atendida previamente.");
            return "redirect:/citas";
        }

        if ("PEDIATRIA".equalsIgnoreCase(cita.getEspecialidad())) {
            ExpedientePediatrico exp = new ExpedientePediatrico();
            exp.setCita(cita);
            exp.setPaciente(cita.getPaciente());
            exp.setMedico(cita.getMedico());
            model.addAttribute("expediente", exp);
            model.addAttribute("historial", clinicoService.obtenerHistorialPediatrico(cita.getPaciente().getIdPaciente()));
            return "consultas/consulta_pediatrica";
        } else {
            ExpedienteOdontopediatrico exp = new ExpedienteOdontopediatrico();
            exp.setCita(cita);
            exp.setPaciente(cita.getPaciente());
            exp.setMedico(cita.getMedico());
            model.addAttribute("expediente", exp);
            model.addAttribute("historial", clinicoService.obtenerHistorialOdontopediatrico(cita.getPaciente().getIdPaciente()));
            return "consultas/consulta_odontopediatrica";
        }
    }

    // Guardar Consulta Pediátrica (CU03)
    @PostMapping("/pediatria/guardar")
    public String guardarConsultaPediatrica(@Valid @ModelAttribute("expediente") ExpedientePediatrico expediente,
                                           BindingResult result,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("historial", clinicoService.obtenerHistorialPediatrico(expediente.getPaciente().getIdPaciente()));
            return "consultas/consulta_pediatrica";
        }

        clinicoService.guardarConsultaPediatrica(expediente);
        redirectAttributes.addFlashAttribute("mensajeExito", "Consulta pediátrica guardada con éxito.");
        return "redirect:/citas";
    }

    // Guardar Consulta Odontopediátrica (CU04)
    @PostMapping("/odontopediatria/guardar")
    public String guardarConsultaOdontopediatrica(@Valid @ModelAttribute("expediente") ExpedienteOdontopediatrico expediente,
                                                 BindingResult result,
                                                 Model model,
                                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("historial", clinicoService.obtenerHistorialOdontopediatrico(expediente.getPaciente().getIdPaciente()));
            return "consultas/consulta_odontopediatrica";
        }

        clinicoService.guardarConsultaOdontopediatrica(expediente);
        redirectAttributes.addFlashAttribute("mensajeExito", "Consulta odontopediátrica registrada con éxito.");
        return "redirect:/citas";
    }
}