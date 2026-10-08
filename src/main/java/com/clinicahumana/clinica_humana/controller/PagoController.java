package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.Pago;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.PagoRepository;
import com.clinicahumana.clinica_humana.service.CitaService;
import com.clinicahumana.clinica_humana.service.ClinicoYPagosService;
import com.clinicahumana.clinica_humana.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pagos")
public class PagoController {

    @Autowired
    private ClinicoYPagosService clinicoService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private UsuarioService usuarioService;

    // Panel Principal de Caja: Muestra historial de pagos y citas listas para cobro
    @GetMapping
    public String panelCaja(Principal principal, Model model) {
        String username = principal.getName();
        Usuario usuarioActual = usuarioService.buscarPorUsername(username).orElse(null);

        List<Pago> todosLosPagos = pagoRepository.findAll();
        List<Cita> todasLasCitas = citaService.listarTodas();

        // Filtrar citas completadas que aún NO han sido pagadas
        List<Long> idsCitasPagadas = todosLosPagos.stream()
                .map(p -> p.getCita().getIdCita())
                .toList();

        List<Cita> citasPendientesCobro = todasLasCitas.stream()
                .filter(c -> "COMPLETADA".equalsIgnoreCase(c.getEstado()))
                .filter(c -> !idsCitasPagadas.contains(c.getIdCita()))
                .collect(Collectors.toList());

        // Si es médico, filtrar para mostrar solo su actividad
        if (usuarioActual != null && "PEDIATRA".equals(usuarioActual.getRol())) {
            todosLosPagos = todosLosPagos.stream()
                    .filter(p -> p.getMedico().getIdUsuario().equals(usuarioActual.getIdUsuario()))
                    .toList();
            citasPendientesCobro = citasPendientesCobro.stream()
                    .filter(c -> c.getMedico().getIdUsuario().equals(usuarioActual.getIdUsuario()))
                    .toList();
        } else if (usuarioActual != null && "ODONTOPEDIATRA".equals(usuarioActual.getRol())) {
            todosLosPagos = todosLosPagos.stream()
                    .filter(p -> p.getMedico().getIdUsuario().equals(usuarioActual.getIdUsuario()))
                    .toList();
            citasPendientesCobro = citasPendientesCobro.stream()
                    .filter(c -> c.getMedico().getIdUsuario().equals(usuarioActual.getIdUsuario()))
                    .toList();
        }

        BigDecimal totalRecaudado = todosLosPagos.stream()
                .map(Pago::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("pagos", todosLosPagos);
        model.addAttribute("citasPendientes", citasPendientesCobro);
        model.addAttribute("totalRecaudado", totalRecaudado);
        model.addAttribute("usuarioActual", usuarioActual);

        return "pagos/caja";
    }

    // Formulario para cobrar una cita completada específica
    @GetMapping("/cobrar/{idCita}")
    public String formularioCobro(@PathVariable("idCita") Long idCita, Model model, RedirectAttributes redirectAttributes) {
        Cita cita = citaService.buscarPorId(idCita).orElse(null);

        if (cita == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "La cita solicitada no existe.");
            return "redirect:/pagos";
        }

        if (pagoRepository.findByCitaIdCita(idCita).isPresent()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Esta cita ya fue cobrada previamente.");
            return "redirect:/pagos";
        }

        Pago pago = new Pago();
        pago.setCita(cita);
        pago.setPaciente(cita.getPaciente());
        pago.setMedico(cita.getMedico());
        pago.setConcepto("Consulta " + cita.getEspecialidad().toLowerCase() + " - " + cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellidos());
        pago.setMontoBase(new BigDecimal("500.00")); // Valor sugerido estándar
        pago.setAplicaIva(false);
        pago.setMetodoPago("EFECTIVO");

        model.addAttribute("pago", pago);
        return "pagos/formulario_cobro";
    }

    // Procesar el guardado del pago de forma segura
    @PostMapping("/guardar")
    public String procesarPago(@ModelAttribute("pago") Pago pago,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {

        // Si la cita o su ID no vienen en el formulario
        if (pago.getCita() == null || pago.getCita().getIdCita() == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "Identificador de cita no válido.");
            return "redirect:/pagos";
        }

        Long idCita = pago.getCita().getIdCita();
        Cita citaExistente = citaService.buscarPorId(idCita).orElse(null);

        if (citaExistente == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "La cita asociada no existe.");
            return "redirect:/pagos";
        }

        // Rehidratar entidades completas desde la BD para evitar nulos
        pago.setCita(citaExistente);
        pago.setPaciente(citaExistente.getPaciente());
        pago.setMedico(citaExistente.getMedico());

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Por favor verifique los campos del cobro.");
            return "redirect:/pagos/cobrar/" + idCita;
        }

        try {
            clinicoService.registrarPago(pago);
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Pago registrado exitosamente por un total de $" + pago.getMontoTotal() + " MXN!");
            // Redirige directamente al comprobante oficial de pago recién emitido
            return "redirect:/pagos/comprobante/" + pago.getIdPago();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al procesar el cobro: " + e.getMessage());
            return "redirect:/pagos/cobrar/" + idCita;
        }
    }

    // Endpoint de Impresión y Visualización del Comprobante de Pago
    @GetMapping("/comprobante/{idPago}")
    public String verComprobantePago(@PathVariable("idPago") Long idPago, Model model, RedirectAttributes redirectAttributes) {
        Pago pago = pagoRepository.findById(idPago).orElse(null);

        if (pago == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "El comprobante de pago solicitado no existe.");
            return "redirect:/pagos";
        }

        model.addAttribute("pago", pago);
        return "pagos/comprobante_pago";
    }

    // Corte de Caja Diario
    @GetMapping("/corte")
    public String corteCajaDiario(@RequestParam(name = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                  Principal principal,
                                  Model model) {
        LocalDate fechaConsulta = (fecha != null) ? fecha : LocalDate.now();
        String username = principal.getName();
        Usuario usuarioActual = usuarioService.buscarPorUsername(username).orElse(null);

        List<Pago> pagosDia = pagoRepository.findAll().stream()
                .filter(p -> p.getFechaPago() != null && p.getFechaPago().toLocalDate().isEqual(fechaConsulta))
                .toList();

        // Si es médico, filtrar solo sus ingresos
        if (usuarioActual != null && ("PEDIATRA".equals(usuarioActual.getRol()) || "ODONTOPEDIATRA".equals(usuarioActual.getRol()))) {
            pagosDia = pagosDia.stream()
                    .filter(p -> p.getMedico().getIdUsuario().equals(usuarioActual.getIdUsuario()))
                    .toList();
        }

        BigDecimal totalEfectivo = pagosDia.stream()
                .filter(p -> "EFECTIVO".equalsIgnoreCase(p.getMetodoPago()))
                .map(Pago::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTarjeta = pagosDia.stream()
                .filter(p -> "TARJETA".equalsIgnoreCase(p.getMetodoPago()))
                .map(Pago::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTransferencia = pagosDia.stream()
                .filter(p -> "TRANSFERENCIA".equalsIgnoreCase(p.getMetodoPago()))
                .map(Pago::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGeneral = totalEfectivo.add(totalTarjeta).add(totalTransferencia);

        model.addAttribute("fechaConsulta", fechaConsulta);
        model.addAttribute("pagosDia", pagosDia);
        model.addAttribute("totalEfectivo", totalEfectivo);
        model.addAttribute("totalTarjeta", totalTarjeta);
        model.addAttribute("totalTransferencia", totalTransferencia);
        model.addAttribute("totalGeneral", totalGeneral);
        model.addAttribute("usuarioActual", usuarioActual);

        return "pagos/corte";
    }
}