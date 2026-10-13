package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.ExpedienteOdontopediatrico;
import com.clinicahumana.clinica_humana.model.ExpedientePediatrico;
import com.clinicahumana.clinica_humana.service.ClinicoYPagosService;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/consultas")
public class ExpedienteClinicoController {

    @Autowired
    private ClinicoYPagosService clinicoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PacienteService pacienteService;

    // ===================== PEDIATRÍA =====================

    // ===================== PEDIATRÍA =====================

    @PostMapping("/pediatria/expediente/guardar")
    public String guardarExpedientePediatrico(@Valid @ModelAttribute("expediente") ExpedientePediatrico expediente,
                                              BindingResult result,
                                              @RequestParam(name = "idCita", required = false) Long idCita,
                                              @RequestParam(name = "idPaciente", required = false) Long idPaciente,
                                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "consultas/expediente_pediatrico_form";
        }

        if (idCita == null) {
            result.rejectValue("cita", "", "Seleccione una cita válida.");
            return "consultas/expediente_pediatrico_form";
        }

        Optional<Cita> citaOpt = clinicoService.buscarCitaPorId(idCita);
        if (citaOpt.isEmpty()) {
            result.rejectValue("cita", "", "La cita seleccionada no existe.");
            return "consultas/expediente_pediatrico_form";
        }

        Cita cita = citaOpt.get();
        if (idPaciente != null && !idPaciente.equals(cita.getPaciente().getIdPaciente())) {
            result.rejectValue("cita", "", "La cita seleccionada no corresponde al paciente indicado.");
            return "consultas/expediente_pediatrico_form";
        }

        Optional<ExpedientePediatrico> existe = clinicoService.buscarExpedientePediatricoPorCita(idCita);
        if (existe.isPresent()) {
            result.rejectValue("cita", "", "Esa cita ya tiene un expediente registrado.");
            return "consultas/expediente_pediatrico_form";
        }

        expediente.setCita(cita);
        expediente.setPaciente(cita.getPaciente());
        expediente.setMedico(cita.getMedico());

        clinicoService.guardarExpedientePediatricoDesdeCrud(expediente, cita);
        redirectAttributes.addFlashAttribute("mensajeExito", "Atención pediátrica registrada exitosamente.");
        return "redirect:/pacientes/expediente/" + cita.getPaciente().getIdPaciente();
    }

    @GetMapping("/pediatria/expediente/editar/{idExpediente}")
    public String formularioEditarExpedientePediatrico(@PathVariable Long idExpediente,
                                                       @RequestParam(name = "idPaciente", required = false) Long idPaciente,
                                                       Principal principal,
                                                       Model model,
                                                       RedirectAttributes redirectAttributes) {
        Optional<ExpedientePediatrico> expOpt = clinicoService.obtenerExpedientePediatricoPorId(idExpediente);
        if (expOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "La atención no existe.");
            return "redirect:/pacientes";
        }

        ExpedientePediatrico exp = expOpt.get();
        Usuario medico = obtenerMedicoAsignado(principal, "PEDIATRA");
        if (medico == null || !medico.getIdUsuario().equals(exp.getMedico().getIdUsuario())) {
            redirectAttributes.addFlashAttribute("mensajeError", "No tienes permiso para editar esta atención.");
            return "redirect:/pacientes";
        }

        model.addAttribute("expediente", exp);
        model.addAttribute("tituloFormulario", "Editar Atención Pediátrica");

        model.addAttribute("urlAction", "/consultas/pediatria/expediente/actualizar");
        model.addAttribute("linkVolver", "/pacientes/expediente/" + exp.getPaciente().getIdPaciente());
        return "consultas/expediente_pediatrico_form";
    }

    @PostMapping("/pediatria/expediente/actualizar")
    public String actualizarExpedientePediatrico(@Valid @ModelAttribute("expediente") ExpedientePediatrico expediente,
                                                 BindingResult result,
                                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "consultas/expediente_pediatrico_form";
        }

        Optional<ExpedientePediatrico> expOpt = clinicoService.obtenerExpedientePediatricoPorId(expediente.getIdExpedientePed());
        if (expOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "La atención no existe.");
            return "redirect:/pacientes";
        }

        ExpedientePediatrico existente = expOpt.get();
        existente.setDiagnostico(expediente.getDiagnostico());
        existente.setTratamiento(expediente.getTratamiento());
        existente.setPeso(expediente.getPeso());
        existente.setTalla(expediente.getTalla());
        existente.setTemperatura(expediente.getTemperatura());
        existente.setPresionArterial(expediente.getPresionArterial());
        existente.setFrecuenciaCardiaca(expediente.getFrecuenciaCardiaca());
        existente.setSaturacionOxigeno(expediente.getSaturacionOxigeno());

        clinicoService.actualizarExpedientePediatrico(existente);
        redirectAttributes.addFlashAttribute("mensajeExito", "Atención pediátrica actualizada exitosamente.");
        return "redirect:/pacientes/expediente/" + existente.getPaciente().getIdPaciente();
    }

    @PostMapping("/pediatria/expediente/eliminar/{idExpediente}")
    public String eliminarExpedientePediatrico(@PathVariable Long idExpediente,
                                               @RequestParam(name = "idPaciente", required = false) Long idPaciente,
                                               Principal principal,
                                               RedirectAttributes redirectAttributes) {
        Optional<ExpedientePediatrico> expOpt = clinicoService.obtenerExpedientePediatricoPorId(idExpediente);
        if (expOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "La atención no existe.");
            return "redirect:/pacientes";
        }

        ExpedientePediatrico exp = expOpt.get();
        Usuario medico = obtenerMedicoAsignado(principal, "PEDIATRA");
        if (medico == null || !medico.getIdUsuario().equals(exp.getMedico().getIdUsuario())) {
            redirectAttributes.addFlashAttribute("mensajeError", "No tienes permiso para eliminar esta atención.");
            return "redirect:/pacientes";
        }

        clinicoService.eliminarExpedientePediatrico(idExpediente);
        redirectAttributes.addFlashAttribute("mensajeExito", "Atención pediátrica eliminada del historial.");
        return "redirect:/pacientes/expediente/" + exp.getPaciente().getIdPaciente();
    }

    // ===================== ODONTOPEDIATRÍA =====================

    @PostMapping("/odontopediatria/expediente/guardar")
    public String guardarExpedienteOdontopediatrico(@Valid @ModelAttribute("expediente") ExpedienteOdontopediatrico expediente,
                                                   BindingResult result,
                                                   @RequestParam(name = "idCita", required = false) Long idCita,
                                                   @RequestParam(name = "idPaciente", required = false) Long idPaciente,
                                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "consultas/expediente_odontopediatrico_form";
        }

        if (idCita == null) {
            result.rejectValue("cita", "", "Seleccione una cita válida.");
            return "consultas/expediente_odontopediatrico_form";
        }

        Optional<Cita> citaOpt = clinicoService.buscarCitaPorId(idCita);
        if (citaOpt.isEmpty()) {
            result.rejectValue("cita", "", "La cita seleccionada no existe.");
            return "consultas/expediente_odontopediatrico_form";
        }

        Cita cita = citaOpt.get();
        if (idPaciente != null && !idPaciente.equals(cita.getPaciente().getIdPaciente())) {
            result.rejectValue("cita", "", "La cita seleccionada no corresponde al paciente indicado.");
            return "consultas/expediente_odontopediatrico_form";
        }

        Optional<ExpedienteOdontopediatrico> existe = clinicoService.buscarExpedienteOdontopediatricoPorCita(idCita);
        if (existe.isPresent()) {
            result.rejectValue("cita", "", "Esa cita ya tiene un expediente registrado.");
            return "consultas/expediente_odontopediatrico_form";
        }

        expediente.setCita(cita);
        expediente.setPaciente(cita.getPaciente());
        expediente.setMedico(cita.getMedico());

        clinicoService.guardarExpedienteOdontopediatricoDesdeCrud(expediente, cita);
        redirectAttributes.addFlashAttribute("mensajeExito", "Atención odontopediátrica registrada exitosamente.");
        return "redirect:/pacientes/expediente/" + cita.getPaciente().getIdPaciente();
    }

    @GetMapping("/odontopediatria/expediente/editar/{idExpediente}")
    public String formularioEditarExpedienteOdontopediatrico(@PathVariable Long idExpediente,
                                                             @RequestParam(name = "idPaciente", required = false) Long idPaciente,
                                                             Principal principal,
                                                             Model model,
                                                             RedirectAttributes redirectAttributes) {
        Optional<ExpedienteOdontopediatrico> expOpt = clinicoService.obtenerExpedienteOdontopediatricoPorId(idExpediente);
        if (expOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "La atención no existe.");
            return "redirect:/pacientes";
        }

        ExpedienteOdontopediatrico exp = expOpt.get();
        Usuario medico = obtenerMedicoAsignado(principal, "ODONTOPEDIATRA");
        if (medico == null || !medico.getIdUsuario().equals(exp.getMedico().getIdUsuario())) {
            redirectAttributes.addFlashAttribute("mensajeError", "No tienes permiso para editar esta atención.");
            return "redirect:/pacientes";
        }

        model.addAttribute("expediente", exp);
        model.addAttribute("tituloFormulario", "Editar Atención Odontopediátrica");

        model.addAttribute("urlAction", "/consultas/odontopediatria/expediente/actualizar");
        model.addAttribute("linkVolver", "/pacientes/expediente/" + exp.getPaciente().getIdPaciente());
        return "consultas/expediente_odontopediatrico_form";
    }

    @PostMapping("/odontopediatria/expediente/actualizar")
    public String actualizarExpedienteOdontopediatrico(@Valid @ModelAttribute("expediente") ExpedienteOdontopediatrico expediente,
                                                      BindingResult result,
                                                      RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "consultas/expediente_odontopediatrico_form";
        }

        Optional<ExpedienteOdontopediatrico> expOpt = clinicoService.obtenerExpedienteOdontopediatricoPorId(expediente.getIdExpedienteOdonto());
        if (expOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "La atención no existe.");
            return "redirect:/pacientes";
        }

        ExpedienteOdontopediatrico existente = expOpt.get();
        existente.setDiagnosticoDental(expediente.getDiagnosticoDental());
        existente.setProcedimientosRealizados(expediente.getProcedimientosRealizados());
        existente.setPiezasTratadas(expediente.getPiezasTratadas());
        existente.setRecomendacionesHigiene(expediente.getRecomendacionesHigiene());
        existente.setCostoTratamiento(expediente.getCostoTratamiento());

        clinicoService.actualizarExpedienteOdontopediatrico(existente);
        redirectAttributes.addFlashAttribute("mensajeExito", "Atención odontopediátrica actualizada exitosamente.");
        return "redirect:/pacientes/expediente/" + existente.getPaciente().getIdPaciente();
    }

    @PostMapping("/odontopediatria/expediente/eliminar/{idExpediente}")
    public String eliminarExpedienteOdontopediatrico(@PathVariable Long idExpediente,
                                                    @RequestParam(name = "idPaciente", required = false) Long idPaciente,
                                                    Principal principal,
                                                    RedirectAttributes redirectAttributes) {
        Optional<ExpedienteOdontopediatrico> expOpt = clinicoService.obtenerExpedienteOdontopediatricoPorId(idExpediente);
        if (expOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "La atención no existe.");
            return "redirect:/pacientes";
        }

        ExpedienteOdontopediatrico exp = expOpt.get();
        Usuario medico = obtenerMedicoAsignado(principal, "ODONTOPEDIATRA");
        if (medico == null || !medico.getIdUsuario().equals(exp.getMedico().getIdUsuario())) {
            redirectAttributes.addFlashAttribute("mensajeError", "No tienes permiso para eliminar esta atención.");
            return "redirect:/pacientes";
        }

        clinicoService.eliminarExpedienteOdontopediatrico(idExpediente);
        redirectAttributes.addFlashAttribute("mensajeExito", "Atención odontopediátrica eliminada del historial.");
        return "redirect:/pacientes/expediente/" + exp.getPaciente().getIdPaciente();
    }

    // ===================== Utilidades =====================

    private Usuario obtenerMedicoAsignado(Principal principal, String rolEsperado) {
        if (principal == null) return null;
        return usuarioService.buscarPorUsername(principal.getName())
                .filter(u -> rolEsperado.equals(u.getRol()))
                .orElse(null);
    }
}
