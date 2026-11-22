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
    public String panelCaja(@RequestParam(name = "especialidad", required = false) String especialidad,
                            Principal principal, Model model) {
        String username = principal.getName();
        Usuario usuarioActual = usuarioService.buscarPorUsername(username).orElse(null);

        List<Pago> todosLosPagos;
        List<Cita> citasPendientesCobro;

        String rolActual = usuarioActual != null ? usuarioActual.getRol() : "";
        // La plantilla usa estos atributos en todos los roles: se declaran siempre
        boolean esAdmin = "ADMIN".equals(rolActual);
        String filtroEspecialidad = (especialidad != null && !especialidad.isBlank())
                ? especialidad.trim().toUpperCase() : null;

        // MÉDICOS (Pediatra/Odontóloga): ven únicamente su propia actividad,
        // filtrada además por especialidad para que cada panel no vea lo del otro.
        if ("PEDIATRA".equals(rolActual) || "ODONTOLOGIA".equals(rolActual)) {
            String miEspecialidad = ("PEDIATRA".equals(rolActual)) ? "PEDIATRIA" : "ODONTOLOGIA";
            todosLosPagos = pagoRepository.findByCitaEspecialidad(miEspecialidad).stream()
                    .filter(p -> p.getMedico() != null && p.getMedico().getIdUsuario().equals(usuarioActual.getIdUsuario()))
                    .toList();
            List<Long> idsCitasPagadas = todosLosPagos.stream()
                    .map(p -> p.getCita() != null ? p.getCita().getIdCita() : null)
                    .filter(id -> id != null)
                    .map(Long::longValue)
                    .toList();
            citasPendientesCobro = clinicoService.citasPendientesPorEspecialidad(miEspecialidad).stream()
                    .filter(c -> c.getMedico().getIdUsuario().equals(usuarioActual.getIdUsuario()))
                    .filter(c -> !idsCitasPagadas.contains(c.getIdCita()))
                    .toList();
        }
        // RECEPCIONISTA: trabaja exclusivamente con citas y cobros de Odontología
        else if ("RECEPCIONISTA".equals(rolActual)) {
            todosLosPagos = pagoRepository.findByCitaEspecialidad("ODONTOLOGIA").stream()
                    .filter(p -> p.getMedico() != null)
                    .toList();
            List<Long> idsCitasPagadas = todosLosPagos.stream()
                    .map(p -> p.getCita() != null ? p.getCita().getIdCita() : null)
                    .filter(id -> id != null)
                    .map(Long::longValue)
                    .toList();
            citasPendientesCobro = clinicoService.citasPendientesPorEspecialidad("ODONTOLOGIA").stream()
                    .filter(c -> !idsCitasPagadas.contains(c.getIdCita()))
                    .toList();
        }
        // ADMIN: vista global con filtro opcional por especialidad
        else {
            todosLosPagos = pagoRepository.findAll();
            citasPendientesCobro = citaService.listarTodas().stream()
                    .filter(c -> "COMPLETADA".equalsIgnoreCase(c.getEstado()))
                    .collect(Collectors.toList());
            List<Long> idsCitasPagadas = todosLosPagos.stream()
                    .map(p -> p.getCita() != null ? p.getCita().getIdCita() : null)
                    .filter(id -> id != null)
                    .map(Long::longValue)
                    .toList();
            citasPendientesCobro = citasPendientesCobro.stream()
                    .filter(c -> !idsCitasPagadas.contains(c.getIdCita()))
                    .toList();
            if (esAdmin && filtroEspecialidad != null) {
                todosLosPagos = todosLosPagos.stream()
                        .filter(p -> p.getCita() != null && filtroEspecialidad.equalsIgnoreCase(p.getCita().getEspecialidad()))
                        .toList();
                citasPendientesCobro = citasPendientesCobro.stream()
                        .filter(c -> filtroEspecialidad.equalsIgnoreCase(c.getEspecialidad()))
                        .toList();
            }
        }

        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("filtroEspecialidad", filtroEspecialidad);

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
        // En odontología el concepto es el servicio dental: el presupuesto ya identifica al paciente.
        pago.setConcepto("ODONTOLOGIA".equalsIgnoreCase(cita.getEspecialidad())
                ? "Atención odontológica integral"
                : "Consulta " + cita.getEspecialidad().toLowerCase() + " - " + cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellidos());
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

        // Odontología (CISO) cobra con el presupuesto oficial de la Dra. Gloria González;
        // Pediatría conserva su comprobante institucional.
        String especialidad = (pago.getCita() != null) ? pago.getCita().getEspecialidad() : "";
        if ("ODONTOLOGIA".equalsIgnoreCase(especialidad)) {
            model.addAttribute("folioPresupuesto", String.format("%06d", pago.getIdPago()));
            return "pagos/presupuesto_odontologia";
        }

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

        String rolActual = usuarioActual != null ? usuarioActual.getRol() : "";
        String miEspecialidad = null;

        // Cada rol define el alcance del corte: ninguno ve los cobros del otro panel.
        if ("PEDIATRA".equals(rolActual) || "ODONTOLOGIA".equals(rolActual)) {
            miEspecialidad = ("PEDIATRA".equals(rolActual)) ? "PEDIATRIA" : "ODONTOLOGIA";
        } else if ("RECEPCIONISTA".equals(rolActual)) {
            miEspecialidad = "ODONTOLOGIA";
        }

        List<Pago> pagosDia;
        if (miEspecialidad != null) {
            pagosDia = pagoRepository.findByCitaEspecialidad(miEspecialidad).stream()
                    .filter(p -> p.getFechaPago() != null && p.getFechaPago().toLocalDate().isEqual(fechaConsulta))
                    .toList();
            if ("PEDIATRA".equals(rolActual) || "ODONTOLOGIA".equals(rolActual)) {
                pagosDia = pagosDia.stream()
                        .filter(p -> p.getMedico() != null && p.getMedico().getIdUsuario().equals(usuarioActual.getIdUsuario()))
                        .toList();
            }
        } else {
            pagosDia = pagoRepository.findAll().stream()
                    .filter(p -> p.getFechaPago() != null && p.getFechaPago().toLocalDate().isEqual(fechaConsulta))
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