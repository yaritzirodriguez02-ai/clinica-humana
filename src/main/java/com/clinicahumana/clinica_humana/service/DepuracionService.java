package com.clinicahumana.clinica_humana.service;

import com.clinicahumana.clinica_humana.model.CarnetCitas;
import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.ExpedienteOdontopediatrico;
import com.clinicahumana.clinica_humana.model.ExpedientePediatrico;
import com.clinicahumana.clinica_humana.model.InformeOdontologico;
import com.clinicahumana.clinica_humana.model.Paciente;
import com.clinicahumana.clinica_humana.model.Pago;
import com.clinicahumana.clinica_humana.model.Receta;
import com.clinicahumana.clinica_humana.model.Tutor;
import com.clinicahumana.clinica_humana.model.Usuario;
import com.clinicahumana.clinica_humana.repository.CarnetCitasRepository;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.ExpedienteOdontopediatricoRepository;
import com.clinicahumana.clinica_humana.repository.ExpedientePediatricoRepository;
import com.clinicahumana.clinica_humana.repository.InformeOdontologicoRepository;
import com.clinicahumana.clinica_humana.repository.PacienteRepository;
import com.clinicahumana.clinica_humana.repository.PagoRepository;
import com.clinicahumana.clinica_humana.repository.RecetaRepository;
import com.clinicahumana.clinica_humana.repository.TutorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Depuración de registros por área clínica.
 *
 * El sistema comparte el catálogo de pacientes entre los dos paneles, por lo que
 * "vaciar un área" significa borrar únicamente los registros clínicos de esa
 * especialidad (citas, consultas, recetas, pagos, informes, carnets) y, además,
 * a los pacientes que no tengan historial en la otra área. Los pacientes que
 * acuden a ambas especialidades se conservan con su historial de la otra área.
 */
@Service
public class DepuracionService {

    /** Las dos áreas clínicas independientes del sistema. */
    public enum Area {
        ODONTOLOGIA("odontologia", "Odontología (CISO)",
                List.of("ODONTOLOGIA", "ODONTOPEDIATRIA")),
        PEDIATRIA("pediatria", "Pediatría",
                List.of("PEDIATRIA"));

        private final String slug;
        private final String etiqueta;
        private final List<String> especialidades;

        Area(String slug, String etiqueta, List<String> especialidades) {
            this.slug = slug;
            this.etiqueta = etiqueta;
            this.especialidades = especialidades;
        }

        public String getSlug() {
            return slug;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public List<String> getEspecialidades() {
            return especialidades;
        }

        public Area otra() {
            return this == ODONTOLOGIA ? PEDIATRIA : ODONTOLOGIA;
        }

        public static Area desdeSlug(String slug) {
            if (slug == null) {
                return null;
            }
            for (Area area : values()) {
                if (area.slug.equalsIgnoreCase(slug.trim())) {
                    return area;
                }
            }
            return null;
        }
    }

    /** Consulta unificada de las dos especialidades para pintar una sola tabla. */
    public record ConsultaResumen(Long id, LocalDateTime fecha, Paciente paciente,
                                  Usuario medico, String diagnostico, boolean odontologica) {
    }

    /** Todo lo que existe hoy en un área, con los pacientes compartidos ya marcados. */
    public record ResumenArea(Area area,
                              List<Cita> citas,
                              List<ConsultaResumen> consultas,
                              List<Receta> recetas,
                              List<Pago> pagos,
                              List<InformeOdontologico> informes,
                              List<CarnetCitas> carnets,
                              List<Paciente> pacientes,
                              Set<Long> pacientesCompartidos) {
    }

    /** Conteo de lo que se borró en un vaciado. */
    public record ResultadoDepuracion(int pacientes, int citas, int consultas, int recetas,
                                      int pagos, int informes, int carnets, int tutores) {
        public int total() {
            return pacientes + citas + consultas + recetas + pagos + informes + carnets + tutores;
        }
    }

    @Autowired
    private CitaRepository citaRepo;

    @Autowired
    private ExpedienteOdontopediatricoRepository expOdontoRepo;

    @Autowired
    private ExpedientePediatricoRepository expPedRepo;

    @Autowired
    private RecetaRepository recetaRepo;

    @Autowired
    private PagoRepository pagoRepo;

    @Autowired
    private InformeOdontologicoRepository informeRepo;

    @Autowired
    private CarnetCitasRepository carnetRepo;

    @Autowired
    private PacienteRepository pacienteRepo;

    @Autowired
    private TutorRepository tutorRepo;

    // ===================== CONSULTA =====================

    @Transactional(readOnly = true)
    public ResumenArea resumen(Area area) {
        List<Cita> citas = citasDelArea(area);
        List<ConsultaResumen> consultas = consultasDelArea(area, idsCitas(citas));

        List<Receta> recetas = new ArrayList<>();
        List<Pago> pagos = new ArrayList<>();
        Set<Long> idsCita = idsCitas(citas);
        if (!idsCita.isEmpty()) {
            recetas.addAll(recetaRepo.findByCitaIdCitaIn(idsCita));
            pagos.addAll(pagoRepo.findByCitaIdCitaIn(idsCita));
        }
        recetas.sort(Comparator.comparing(Receta::getFechaEmision,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                .thenComparing(Receta::getIdReceta, Comparator.nullsLast(Comparator.reverseOrder())));
        pagos.sort(Comparator.comparing(Pago::getFechaPago,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<InformeOdontologico> informes = List.of();
        List<CarnetCitas> carnets = List.of();
        if (area == Area.ODONTOLOGIA) {
            informes = informeRepo.findAllByOrderByFechaEmisionDescIdInformeDesc();
            carnets = new ArrayList<>(carnetRepo.findAll());
            carnets.sort(Comparator.comparing(CarnetCitas::getFecha,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        }

        Map<Long, Paciente> pacientes = new LinkedHashMap<>();
        for (Cita c : citas) {
            pacientes.putIfAbsent(c.getPaciente().getIdPaciente(), c.getPaciente());
        }
        for (ConsultaResumen c : consultas) {
            pacientes.putIfAbsent(c.paciente().getIdPaciente(), c.paciente());
        }
        for (InformeOdontologico i : informes) {
            pacientes.putIfAbsent(i.getPaciente().getIdPaciente(), i.getPaciente());
        }
        for (CarnetCitas c : carnets) {
            pacientes.putIfAbsent(c.getPaciente().getIdPaciente(), c.getPaciente());
        }

        List<Paciente> listaPacientes = new ArrayList<>(pacientes.values());
        listaPacientes.sort(Comparator.comparing(Paciente::getNombre, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Paciente::getApellidos, String.CASE_INSENSITIVE_ORDER));

        Set<Long> compartidos = new LinkedHashSet<>();
        for (Paciente p : listaPacientes) {
            if (tieneRegistrosEn(p, area.otra())) {
                compartidos.add(p.getIdPaciente());
            }
        }

        return new ResumenArea(area, citas, consultas, recetas, pagos, informes, carnets,
                listaPacientes, compartidos);
    }

    // ===================== VACIADO DEL ÁREA =====================

    @Transactional
    public ResultadoDepuracion vaciarArea(Area area) {
        List<Cita> citas = citasDelArea(area);
        Set<Long> idsCita = idsCitas(citas);

        List<ConsultaResumen> consultas = consultasDelArea(area, idsCita);

        Map<Long, Paciente> pacientesDelArea = new LinkedHashMap<>();
        for (Cita c : citas) {
            pacientesDelArea.putIfAbsent(c.getPaciente().getIdPaciente(), c.getPaciente());
        }
        for (ConsultaResumen c : consultas) {
            pacientesDelArea.putIfAbsent(c.paciente().getIdPaciente(), c.paciente());
        }

        // Informes y carnets son exclusivos de odontología: se recogen antes de borrarlos
        // para conservar la referencia de sus pacientes.
        List<InformeOdontologico> informes = List.of();
        List<CarnetCitas> carnets = List.of();
        if (area == Area.ODONTOLOGIA) {
            informes = informeRepo.findAll();
            carnets = carnetRepo.findAll();
            for (InformeOdontologico i : informes) {
                pacientesDelArea.putIfAbsent(i.getPaciente().getIdPaciente(), i.getPaciente());
            }
            for (CarnetCitas c : carnets) {
                pacientesDelArea.putIfAbsent(c.getPaciente().getIdPaciente(), c.getPaciente());
            }
        }

        // Los compartidos se calculan ANTES de borrar: si el paciente también tiene
        // historial en la otra área, su expediente se conserva.
        Set<Long> compartidos = new LinkedHashSet<>();
        for (Paciente p : pacientesDelArea.values()) {
            if (tieneRegistrosEn(p, area.otra())) {
                compartidos.add(p.getIdPaciente());
            }
        }

        // 1. Consultas del área
        if (area == Area.ODONTOLOGIA) {
            List<ExpedienteOdontopediatrico> exps = idsCita.isEmpty()
                    ? List.of() : expOdontoRepo.findByCitaIdCitaIn(idsCita);
            expOdontoRepo.deleteAll(exps);
            expOdontoRepo.flush();
        } else {
            List<ExpedientePediatrico> exps = idsCita.isEmpty()
                    ? List.of() : expPedRepo.findByCitaIdCitaIn(idsCita);
            expPedRepo.deleteAll(exps);
            expPedRepo.flush();
        }

        // 2. Recetas y pagos ligados a las citas del área
        List<Receta> recetas = idsCita.isEmpty()
                ? List.of() : recetaRepo.findByCitaIdCitaIn(idsCita);
        recetaRepo.deleteAll(recetas);
        recetaRepo.flush();

        List<Pago> pagos = idsCita.isEmpty()
                ? List.of() : pagoRepo.findByCitaIdCitaIn(idsCita);
        pagoRepo.deleteAll(pagos);
        pagoRepo.flush();

        // 3. Documentos exclusivos de odontología
        if (area == Area.ODONTOLOGIA) {
            informeRepo.deleteAll(informes);
            informeRepo.flush();
            carnetRepo.deleteAll(carnets);
            carnetRepo.flush();
        } else {
            // Si un informe odontológico apuntaba a una cita pediátrica, solo se desvincula.
            List<InformeOdontologico> vinculados = idsCita.isEmpty()
                    ? List.of() : informeRepo.findByCitaIdCitaIn(idsCita);
            for (InformeOdontologico i : vinculados) {
                i.setCita(null);
            }
            informeRepo.saveAll(vinculados);
            informeRepo.flush();
        }

        // 4. Citas del área
        citaRepo.deleteAll(citas);
        citaRepo.flush();

        // 5. Pacientes que solo existían en esta área + tutores que quedan sin hijos
        List<Paciente> exclusivos = new ArrayList<>();
        Set<Long> idsTutores = new LinkedHashSet<>();
        for (Paciente p : pacientesDelArea.values()) {
            if (compartidos.contains(p.getIdPaciente())) {
                continue;
            }
            exclusivos.add(p);
            if (p.getTutor() != null && p.getTutor().getIdTutor() != null) {
                idsTutores.add(p.getTutor().getIdTutor());
            }
        }
        // Un paciente exclusivo del área puede tener todavía un carnet o un informe de la
        // otra especialidad (documentos sin cita). Se borran con él para no dejar huérfanos.
        for (Paciente p : exclusivos) {
            informeRepo.deleteAll(informeRepo.findByPacienteIdPaciente(p.getIdPaciente()));
            carnetRepo.deleteAll(carnetRepo.findByPacienteIdPaciente(p.getIdPaciente()));
        }
        informeRepo.flush();
        carnetRepo.flush();
        pacienteRepo.deleteAll(exclusivos);
        pacienteRepo.flush();
        int tutoresEliminados = limpiarTutores(idsTutores);

        return new ResultadoDepuracion(exclusivos.size(), citas.size(), consultas.size(),
                recetas.size(), pagos.size(), informes.size(), carnets.size(), tutoresEliminados);
    }

    // ===================== BORRADO INDIVIDUAL =====================

    @Transactional
    public void eliminarCita(Area area, Long idCita) {
        Cita cita = citaRepo.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("La cita ya no existe."));
        validarArea(area, cita.getEspecialidad());

        // Los informes son documentos clínicos: se conservan sin la cita de referencia.
        List<InformeOdontologico> informes = informeRepo.findByCitaIdCita(idCita);
        for (InformeOdontologico i : informes) {
            i.setCita(null);
        }
        informeRepo.saveAll(informes);
        informeRepo.flush();

        recetaRepo.deleteAll(recetaRepo.findByCitaIdCita(idCita));
        recetaRepo.flush();
        pagoRepo.findByCitaIdCita(idCita).ifPresent(pagoRepo::delete);
        pagoRepo.flush();
        expOdontoRepo.findByCitaIdCita(idCita).ifPresent(expOdontoRepo::delete);
        expOdontoRepo.flush();
        expPedRepo.findByCitaIdCita(idCita).ifPresent(expPedRepo::delete);
        expPedRepo.flush();

        citaRepo.delete(cita);
        citaRepo.flush();
    }

    @Transactional
    public void eliminarConsulta(Area area, Long idConsulta) {
        if (area == Area.ODONTOLOGIA) {
            ExpedienteOdontopediatrico exp = expOdontoRepo.findById(idConsulta)
                    .orElseThrow(() -> new IllegalArgumentException("La consulta ya no existe."));
            validarArea(area, exp.getCita().getEspecialidad());
            expOdontoRepo.delete(exp);
            expOdontoRepo.flush();
        } else {
            ExpedientePediatrico exp = expPedRepo.findById(idConsulta)
                    .orElseThrow(() -> new IllegalArgumentException("La consulta ya no existe."));
            validarArea(area, exp.getCita().getEspecialidad());
            expPedRepo.delete(exp);
            expPedRepo.flush();
        }
    }

    @Transactional
    public void eliminarReceta(Area area, Long idReceta) {
        Receta receta = recetaRepo.findById(idReceta)
                .orElseThrow(() -> new IllegalArgumentException("La receta ya no existe."));
        validarArea(area, receta.getCita().getEspecialidad());
        recetaRepo.delete(receta);
        recetaRepo.flush();
    }

    @Transactional
    public void eliminarPago(Area area, Long idPago) {
        Pago pago = pagoRepo.findById(idPago)
                .orElseThrow(() -> new IllegalArgumentException("El cobro ya no existe."));
        validarArea(area, pago.getCita().getEspecialidad());
        pagoRepo.delete(pago);
        pagoRepo.flush();
    }

    @Transactional
    public void eliminarInforme(Area area, Long idInforme) {
        if (area != Area.ODONTOLOGIA) {
            throw new IllegalArgumentException("Los informes membretados pertenecen al área de odontología.");
        }
        if (!informeRepo.existsById(idInforme)) {
            throw new IllegalArgumentException("El informe ya no existe.");
        }
        informeRepo.deleteById(idInforme);
        informeRepo.flush();
    }

    @Transactional
    public void eliminarCarnet(Area area, Long idCarnet) {
        if (area != Area.ODONTOLOGIA) {
            throw new IllegalArgumentException("Los carnets pertenecen al área de odontología.");
        }
        if (!carnetRepo.existsById(idCarnet)) {
            throw new IllegalArgumentException("El carnet ya no existe.");
        }
        carnetRepo.deleteById(idCarnet);
        carnetRepo.flush();
    }

    /**
     * Elimina a un paciente con todos sus registros de ambas especialidades.
     * Para no destruir el historial del otro panel, un paciente compartido solo
     * puede eliminarse por el administrador (permitirCompartido = true).
     */
    @Transactional
    public void eliminarPaciente(Area area, Long idPaciente, boolean permitirCompartido) {
        Paciente paciente = pacienteRepo.findById(idPaciente)
                .orElseThrow(() -> new IllegalArgumentException("El paciente ya no existe."));

        if (!tieneRegistrosEn(paciente, area)) {
            throw new IllegalArgumentException("El paciente no tiene registros en esta área.");
        }

        boolean compartido = tieneRegistrosEn(paciente, area.otra());
        if (compartido && !permitirCompartido) {
            throw new IllegalStateException("El paciente también tiene historial en el otro panel. "
                    + "Vacía primero los registros de cada área o pide al administrador eliminarlo por completo.");
        }

        eliminarTodoDePaciente(paciente);
    }

    // ===================== APOYO =====================

    private void eliminarTodoDePaciente(Paciente paciente) {
        Long id = paciente.getIdPaciente();

        informeRepo.deleteAll(informeRepo.findByPacienteIdPaciente(id));
        informeRepo.flush();
        recetaRepo.deleteAll(recetaRepo.findByPacienteIdPacienteOrderByFechaEmisionDesc(id));
        recetaRepo.flush();
        pagoRepo.deleteAll(pagoRepo.findByPacienteIdPaciente(id));
        pagoRepo.flush();
        expOdontoRepo.deleteAll(expOdontoRepo.findByPacienteIdPacienteOrderByFechaAtencionDesc(id));
        expOdontoRepo.flush();
        expPedRepo.deleteAll(expPedRepo.findByPacienteIdPacienteOrderByFechaAtencionDesc(id));
        expPedRepo.flush();
        citaRepo.deleteAll(citaRepo.findByPacienteIdPaciente(id));
        citaRepo.flush();
        carnetRepo.deleteAll(carnetRepo.findByPacienteIdPaciente(id));
        carnetRepo.flush();

        Tutor tutor = paciente.getTutor();
        Long idTutor = tutor != null ? tutor.getIdTutor() : null;

        pacienteRepo.delete(paciente);
        pacienteRepo.flush();

        if (idTutor != null && !pacienteRepo.existsByTutorIdTutor(idTutor)) {
            tutorRepo.deleteById(idTutor);
        }
    }

    private int limpiarTutores(Set<Long> idsTutores) {
        int eliminados = 0;
        for (Long idTutor : idsTutores) {
            if (!pacienteRepo.existsByTutorIdTutor(idTutor)) {
                tutorRepo.deleteById(idTutor);
                eliminados++;
            }
        }
        return eliminados;
    }

    private void validarArea(Area area, String especialidad) {
        if (especialidad == null || !area.getEspecialidades().contains(especialidad.toUpperCase())) {
            throw new IllegalArgumentException("El registro no pertenece al área seleccionada.");
        }
    }

    private boolean tieneRegistrosEn(Paciente paciente, Area area) {
        Long id = paciente.getIdPaciente();
        if (citaRepo.existsByPacienteIdPacienteAndEspecialidadIn(id, area.getEspecialidades())) {
            return true;
        }
        return area == Area.ODONTOLOGIA
                ? expOdontoRepo.existsByPacienteIdPaciente(id)
                : expPedRepo.existsByPacienteIdPaciente(id);
    }

    private List<Cita> citasDelArea(Area area) {
        List<Cita> citas = new ArrayList<>(citaRepo.findByEspecialidadIn(area.getEspecialidades()));
        citas.sort(Comparator.comparing(Cita::getFecha,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Cita::getHora, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed());
        return citas;
    }

    private List<ConsultaResumen> consultasDelArea(Area area, Set<Long> idsCita) {
        List<ConsultaResumen> consultas = new ArrayList<>();
        if (idsCita.isEmpty()) {
            return consultas;
        }
        if (area == Area.ODONTOLOGIA) {
            for (ExpedienteOdontopediatrico e : expOdontoRepo.findByCitaIdCitaIn(idsCita)) {
                consultas.add(new ConsultaResumen(e.getIdExpedienteOdonto(), e.getFechaAtencion(),
                        e.getPaciente(), e.getMedico(), e.getDiagnosticoDental(), true));
            }
        } else {
            for (ExpedientePediatrico e : expPedRepo.findByCitaIdCitaIn(idsCita)) {
                consultas.add(new ConsultaResumen(e.getIdExpedientePed(), e.getFechaAtencion(),
                        e.getPaciente(), e.getMedico(), e.getDiagnostico(), false));
            }
        }
        consultas.sort(Comparator.comparing(ConsultaResumen::fecha,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return consultas;
    }

    private Set<Long> idsCitas(List<Cita> citas) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Cita c : citas) {
            ids.add(c.getIdCita());
        }
        return ids;
    }
}
