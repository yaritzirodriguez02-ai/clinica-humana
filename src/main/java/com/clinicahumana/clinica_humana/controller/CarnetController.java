package com.clinicahumana.clinica_humana.controller;

import com.clinicahumana.clinica_humana.model.AreaClinica;
import com.clinicahumana.clinica_humana.model.CarnetCitas;
import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.ExpedienteOdontopediatrico;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Pago;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.ExpedienteOdontopediatricoRepository;
import com.clinicahumana.clinica_humana.repository.PagoRepository;
import com.clinicahumana.clinica_humana.service.CarnetCitasService;
import com.clinicahumana.clinica_humana.service.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
 * Es la copia que conserva la doctora, una por paciente, con el control de
 * próximas citas, tratamiento a realizar y total cobrado; distinta del carnet
 * físico que se lleva el paciente para su propio seguimiento.
 *
 * La doctora anota las próximas citas aquí mismo (tabla carnet_citas); además
 * el carnet muestra las citas de odontología que ya están en la agenda.
 *
 * El documento impreso se arma como tarjetas horizontales de media carta:
 * portada, relación de próximas citas y, al final, el aviso "¡¡IMPORTANTE!!".
 */
@Controller
@RequestMapping("/carnets")
public class CarnetController {

    /** Renglones impresos en cada tarjeta de próximas citas. */
    private static final int RENGLONES_POR_PANEL = 6;

    /** Tarjetas de media carta por hoja tamaño carta. */
    private static final int PANELES_POR_HOJA = 2;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private CarnetCitasService carnetCitasService;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ExpedienteOdontopediatricoRepository expedienteOdontoRepository;

    /** Sitio web que se imprime en la portada. Vacío mientras el sitio no esté publicado. */
    @Value("${ciso.sitio.web:}")
    private String sitioWeb;

    // ===================== CARNET =====================

    @GetMapping("/odontologia/{idPaciente}")
    public String carnetOdontologia(@PathVariable("idPaciente") Long idPaciente,
                                    @RequestParam(name = "editar", required = false) Long idEditar,
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

        List<CarnetCitas> anotaciones = carnetCitasService.listarPorPaciente(idPaciente).stream()
                .sorted(Comparator.comparing(CarnetCitas::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CarnetCitas::getHora, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<Cita> citasAgenda = citasOdontologia(idPaciente);
        List<FilaCarnet> filas = combinarFilas(anotaciones, citasAgenda);

        model.addAttribute("paciente", paciente);
        model.addAttribute("doctora", nombreDoctora(citasAgenda));
        model.addAttribute("sitioWeb", sitioWeb != null ? sitioWeb.trim() : "");
        model.addAttribute("fechaHoy", LocalDate.now());
        model.addAttribute("hojas", armarHojas(filas));
        model.addAttribute("anotaciones", anotaciones);
        model.addAttribute("citasAgenda", citasAgenda);
        model.addAttribute("totalGeneral", filas.stream()
                .map(FilaCarnet::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Renglón que se está editando (si viene ?editar=ID)
        if (idEditar != null) {
            anotaciones.stream()
                    .filter(a -> a.getIdCarnet().equals(idEditar))
                    .findFirst()
                    .ifPresent(a -> model.addAttribute("anotacionEdicion", a));
        }

        return "carnets/carnet_odontologia_print";
    }

    /** Anota una próxima cita en el carnet. Puede tomar los datos de una cita de la agenda. */
    @PostMapping("/odontologia/{idPaciente}/agregar")
    public String agregarAnotacion(@PathVariable("idPaciente") Long idPaciente,
                                   @RequestParam(name = "fecha", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                   @RequestParam(name = "hora", required = false)
                                   @DateTimeFormat(pattern = "HH:mm") LocalTime hora,
                                   @RequestParam(name = "tratamiento", required = false) String tratamiento,
                                   @RequestParam(name = "total", required = false) BigDecimal total,
                                   @RequestParam(name = "idCita", required = false) Long idCita,
                                   RedirectAttributes redirectAttributes) {
        Paciente paciente = pacienteService.buscarPorId(idPaciente).orElse(null);
        if (paciente == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "El paciente solicitado no existe.");
            return "redirect:/pacientes";
        }

        // Si se elige una cita de la agenda, se toman de ahí fecha, hora y tratamiento.
        if (idCita != null) {
            Cita cita = citaRepository.findById(idCita).orElse(null);
            if (cita != null && cita.getPaciente().getIdPaciente().equals(idPaciente)) {
                fecha = cita.getFecha();
                hora = cita.getHora();
                if (tratamiento == null || tratamiento.isBlank()) {
                    tratamiento = cita.getMotivo();
                }
                if (total == null) {
                    total = pagoRepository.findByCitaIdCita(cita.getIdCita())
                            .map(Pago::getMontoTotal)
                            .orElse(null);
                }
            }
        }

        if (fecha == null) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Indica la fecha de la próxima cita para anotarla en el carnet.");
            return "redirect:/carnets/odontologia/" + idPaciente;
        }
        if (tratamiento == null || tratamiento.isBlank()) {
            tratamiento = "Consulta odontológica";
        }

        CarnetCitas anotacion = new CarnetCitas(paciente, fecha, hora, tratamiento.trim(), total);
        carnetCitasService.guardarCarnet(anotacion);

        redirectAttributes.addFlashAttribute("mensajeExito", "Próxima cita anotada en el carnet.");
        return "redirect:/carnets/odontologia/" + idPaciente;
    }

    /** Guarda los cambios de un renglón ya anotado. */
    @PostMapping("/odontologia/{idPaciente}/actualizar/{idCarnet}")
    public String actualizarAnotacion(@PathVariable("idPaciente") Long idPaciente,
                                      @PathVariable("idCarnet") Long idCarnet,
                                      @RequestParam(name = "fecha", required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                      @RequestParam(name = "hora", required = false)
                                      @DateTimeFormat(pattern = "HH:mm") LocalTime hora,
                                      @RequestParam(name = "tratamiento", required = false) String tratamiento,
                                      @RequestParam(name = "total", required = false) BigDecimal total,
                                      RedirectAttributes redirectAttributes) {
        CarnetCitas anotacion = carnetCitasService.buscarPorId(idCarnet).orElse(null);
        if (anotacion == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "La anotación del carnet ya no existe.");
            return "redirect:/carnets/odontologia/" + idPaciente;
        }

        if (fecha == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "La fecha de la cita es obligatoria.");
            return "redirect:/carnets/odontologia/" + idPaciente + "?editar=" + idCarnet;
        }

        anotacion.setFecha(fecha);
        anotacion.setHora(hora);
        anotacion.setTratamientoRealizar(tratamiento == null || tratamiento.isBlank()
                ? "Consulta odontológica" : tratamiento.trim());
        anotacion.setTotalPago(total);
        carnetCitasService.guardarCarnet(anotacion);

        redirectAttributes.addFlashAttribute("mensajeExito", "Anotación del carnet actualizada.");
        return "redirect:/carnets/odontologia/" + idPaciente;
    }

    /** Quita un renglón anotado en el carnet. */
    @PostMapping("/odontologia/{idPaciente}/eliminar/{idCarnet}")
    public String eliminarAnotacion(@PathVariable("idPaciente") Long idPaciente,
                                    @PathVariable("idCarnet") Long idCarnet,
                                    RedirectAttributes redirectAttributes) {
        carnetCitasService.eliminarCarnet(idCarnet);
        redirectAttributes.addFlashAttribute("mensajeExito", "Anotación eliminada del carnet.");
        return "redirect:/carnets/odontologia/" + idPaciente;
    }

    // ===================== APOYO =====================

    private List<Cita> citasOdontologia(Long idPaciente) {
        List<Cita> citas = new ArrayList<>(
                citaRepository.findByPacienteIdPacienteAndEspecialidadIn(idPaciente, List.of("ODONTOLOGIA")));
        citas.removeIf(c -> "CANCELADA".equalsIgnoreCase(c.getEstado()));
        citas.sort(Comparator.comparing(Cita::getFecha).thenComparing(Cita::getHora));
        return citas;
    }

    /**
     * Renglones del carnet: lo que la doctora anotó más las citas de la agenda que
     * todavía no tienen anotación (se comparan por fecha y hora para no duplicar).
     */
    private List<FilaCarnet> combinarFilas(List<CarnetCitas> anotaciones, List<Cita> citas) {
        List<FilaCarnet> filas = new ArrayList<>();

        for (CarnetCitas a : anotaciones) {
            filas.add(new FilaCarnet(null, a.getFecha(), a.getHora(),
                    a.getTratamientoRealizar(), a.getTotalPago(), FilaCarnet.CARNET));
        }

        for (Cita c : citas) {
            boolean yaAnotada = anotaciones.stream().anyMatch(a ->
                    a.getFecha() != null && a.getFecha().equals(c.getFecha())
                            && Objects.equals(a.getHora(), c.getHora()));
            if (yaAnotada) {
                continue;
            }

            String tratamiento = expedienteOdontoRepository.findByCitaIdCita(c.getIdCita())
                    .map(ExpedienteOdontopediatrico::getProcedimientosRealizados)
                    .filter(texto -> texto != null && !texto.isBlank())
                    .orElse(c.getMotivo());
            BigDecimal total = pagoRepository.findByCitaIdCita(c.getIdCita())
                    .map(Pago::getMontoTotal)
                    .orElse(null);

            filas.add(new FilaCarnet(c.getIdCita(), c.getFecha(), c.getHora(),
                    tratamiento, total, FilaCarnet.AGENDA));
        }

        filas.sort(Comparator.comparing(FilaCarnet::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(FilaCarnet::getHora, Comparator.nullsLast(Comparator.naturalOrder())));
        return filas;
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
     * Arma el carnet impreso en el orden del formato físico:
     * portada → relación de próximas citas → IMPORTANTE al final.
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

    /** Renglón del carnet: próxima cita, tratamiento y total. */
    public static class FilaCarnet {

        public static final String CARNET = "CARNET";
        public static final String AGENDA = "AGENDA";

        private final Long idCita;
        private final LocalDate fecha;
        private final LocalTime hora;
        private final String tratamiento;
        private final BigDecimal total;
        private final String origen;

        public FilaCarnet(Long idCita, LocalDate fecha, LocalTime hora,
                          String tratamiento, BigDecimal total, String origen) {
            this.idCita = idCita;
            this.fecha = fecha;
            this.hora = hora;
            this.tratamiento = tratamiento;
            this.total = total;
            this.origen = origen;
        }

        public Long getIdCita() { return idCita; }
        public LocalDate getFecha() { return fecha; }
        public LocalTime getHora() { return hora; }
        public String getTratamiento() { return tratamiento; }
        public BigDecimal getTotal() { return total; }
        public String getOrigen() { return origen; }
    }
}
