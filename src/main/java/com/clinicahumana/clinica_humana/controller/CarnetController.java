package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.AreaClinica;
import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.ExpedienteOdontopediatrico;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Pago;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.ExpedienteOdontopediatricoRepository;
import com.clinicahumana.clinica_humana.repository.PagoRepository;
import com.clinicahumana.clinica_humana.service.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Carnet de citas del consultorio odontológico (CISO).
 *
 * Es la copia digital que conserva la doctora, una por paciente, con el control
 * de próximas citas, tratamiento a realizar y total cobrado; distinta del carnet
 * físico que se lleva el paciente para su propio seguimiento.
 *
 * El documento está armado como tarjeta plegable horizontal de media carta:
 * primero la portada, después la relación de próximas citas y, al final, el
 * aviso "¡¡IMPORTANTE!!" que cierra el carnet.
 */
@Controller
@RequestMapping("/carnets")
public class CarnetController {

    /** Renglones impresos en cada panel de próximas citas. */
    private static final int RENGLONES_POR_PANEL = 6;

    /** Paneles (media carta horizontal) por hoja tamaño carta. */
    private static final int PANELES_POR_HOJA = 2;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ExpedienteOdontopediatricoRepository expedienteOdontoRepository;

    /** Sitio web que se imprime en la portada. Vacío mientras el sitio no esté publicado. */
    @Value("${ciso.sitio.web:}")
    private String sitioWeb;

    @GetMapping("/odontologia/{idPaciente}")
    public String carnetOdontologia(@PathVariable("idPaciente") Long idPaciente,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        Paciente paciente = pacienteService.buscarPorId(idPaciente).orElse(null);
        if (paciente == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "El paciente solicitado no existe.");
            return "redirect:/pacientes";
        }

        if (paciente.getArea() == AreaClinica.PEDIATRIA) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "El carnet de citas del consultorio es exclusivo de los pacientes de odontología.");
            return "redirect:/pacientes/expediente/" + idPaciente;
        }

        List<Cita> citas = new ArrayList<>(
                citaRepository.findByPacienteIdPacienteAndEspecialidadIn(idPaciente, List.of("ODONTOLOGIA")));
        citas.sort(Comparator.comparing(Cita::getFecha).thenComparing(Cita::getHora));

        List<FilaCarnet> filas = new ArrayList<>();
        for (Cita cita : citas) {
            String tratamiento = expedienteOdontoRepository.findByCitaIdCita(cita.getIdCita())
                    .map(ExpedienteOdontopediatrico::getProcedimientosRealizados)
                    .filter(texto -> texto != null && !texto.isBlank())
                    .orElse(cita.getMotivo());

            BigDecimal total = pagoRepository.findByCitaIdCita(cita.getIdCita())
                    .map(Pago::getMontoTotal)
                    .orElse(null);

            filas.add(new FilaCarnet(cita.getFecha(), cita.getHora(), tratamiento, total));
        }

        model.addAttribute("paciente", paciente);
        model.addAttribute("doctora", nombreDoctora(citas));
        model.addAttribute("sitioWeb", sitioWeb != null ? sitioWeb.trim() : "");
        model.addAttribute("fechaHoy", LocalDate.now());
        model.addAttribute("hojas", armarHojas(filas));
        model.addAttribute("totalGeneral", filas.stream()
                .map(FilaCarnet::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return "carnets/carnet_odontologia_print";
    }

    /** Nombre del profesional que atendió, sin el cargo pegado al apellido. */
    private String nombreDoctora(List<Cita> citas) {
        if (!citas.isEmpty() && citas.get(citas.size() - 1).getMedico() != null) {
            String nombre = citas.get(citas.size() - 1).getMedico().getNombreCompleto();
            if (nombre != null && !nombre.isBlank()) {
                return nombre.contains(" - ") ? nombre.substring(0, nombre.indexOf(" - ")).trim() : nombre.trim();
            }
        }
        return "DRA. GLORIA GONZÁLEZ HERRERA";
    }

    /**
     * Arma el carnet en el orden del formato físico:
     * portada → relación de próximas citas (una o varias tarjetas) → IMPORTANTE al final.
     */
    private List<List<PanelCarnet>> armarHojas(List<FilaCarnet> filas) {
        List<PanelCarnet> paneles = new ArrayList<>();
        paneles.add(new PanelCarnet(PanelCarnet.PORTADA, List.of(), BigDecimal.ZERO));

        for (int i = 0; i < filas.size(); i += RENGLONES_POR_PANEL) {
            List<FilaCarnet> tramo = new ArrayList<>(filas.subList(i, Math.min(i + RENGLONES_POR_PANEL, filas.size())));
            BigDecimal subtotal = tramo.stream()
                    .map(FilaCarnet::getTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            paneles.add(new PanelCarnet(PanelCarnet.CITAS, tramo, subtotal));
        }
        if (filas.isEmpty()) {
            paneles.add(new PanelCarnet(PanelCarnet.CITAS, List.of(), BigDecimal.ZERO));
        }

        // El aviso cierra siempre el carnet (queda en la última tarjeta).
        paneles.add(new PanelCarnet(PanelCarnet.IMPORTANTE, List.of(), BigDecimal.ZERO));

        List<List<PanelCarnet>> hojas = new ArrayList<>();
        for (int i = 0; i < paneles.size(); i += PANELES_POR_HOJA) {
            hojas.add(paneles.subList(i, Math.min(i + PANELES_POR_HOJA, paneles.size())));
        }
        return hojas;
    }

    /** Tarjeta impresa del carnet: portada, próximas citas o aviso importante. */
    public static class PanelCarnet {

        public static final String PORTADA = "PORTADA";
        public static final String CITAS = "CITAS";
        public static final String IMPORTANTE = "IMPORTANTE";

        private final String tipo;
        private final List<FilaCarnet> filas;
        private final BigDecimal subtotal;

        public PanelCarnet(String tipo, List<FilaCarnet> filas, BigDecimal subtotal) {
            this.tipo = tipo;
            this.filas = filas;
            this.subtotal = subtotal;
        }

        public String getTipo() { return tipo; }
        public List<FilaCarnet> getFilas() { return filas; }
        public BigDecimal getSubtotal() { return subtotal; }
    }

    /** Renglón impreso en el carnet: próxima cita, tratamiento y total cobrado. */
    public static class FilaCarnet {

        private final LocalDate fecha;
        private final LocalTime hora;
        private final String tratamiento;
        private final BigDecimal total;

        public FilaCarnet(LocalDate fecha, LocalTime hora, String tratamiento, BigDecimal total) {
            this.fecha = fecha;
            this.hora = hora;
            this.tratamiento = tratamiento;
            this.total = total;
        }

        public LocalDate getFecha() { return fecha; }
        public LocalTime getHora() { return hora; }
        public String getTratamiento() { return tratamiento; }
        public BigDecimal getTotal() { return total; }
    }
}
