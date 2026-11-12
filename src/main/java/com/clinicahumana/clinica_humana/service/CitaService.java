package com.clinicahumana.clinica_humana.service;

import com.clinicahumana.clinica_humana.model.AreaClinica;
import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.DisponibilidadHorario;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.DisponibilidadHorarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CitaService {

    /** Mensaje único de choque de horario solicitado para todos los paneles. */
    public static final String MENSAJE_HORARIO_OCUPADO =
            "El horario seleccionado ya se encuentra ocupado para este médico. Por favor, elija otra hora o fecha.";


    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private DisponibilidadHorarioRepository disponibilidadRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Candado de la agenda: serializa la verificación de horario y el INSERT.
     * Evita que dos envíos simultáneos del mismo formulario (doble clic) pasen
     * ambos la validación y creen la cita por duplicado.
     */
    private final Object candadoAgenda = new Object();

    public List<Cita> listarTodas() {
        return citaRepository.findAll();
    }

    public Optional<Cita> buscarPorId(Long id) {
        return citaRepository.findById(id);
    }

    public List<Cita> listarPorEspecialidadYFecha(String especialidad, LocalDate fecha) {
        return citaRepository.findByFechaAndEspecialidad(fecha, especialidad);
    }

    public List<Cita> listarPorMedicoYFecha(Long idMedico, LocalDate fecha) {
        return citaRepository.findByMedicoIdUsuarioAndFecha(idMedico, fecha);
    }

    public List<Cita> listarPorPaciente(Long idPaciente) {
        return citaRepository.findByPacienteIdPaciente(idPaciente);
    }

    // ============ Listados de agenda (próximas citas a partir de hoy) ============

    public List<Cita> listarProximas(LocalDate fechaDesde) {
        return citaRepository.findByFechaGreaterThanEqualOrderByFechaAscHoraAsc(fechaDesde);
    }

    public List<Cita> listarProximasPorMedico(Long idMedico, LocalDate fechaDesde) {
        return citaRepository.findByMedicoIdUsuarioAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(idMedico, fechaDesde);
    }

    public List<Cita> listarProximasPorEspecialidad(String especialidad, LocalDate fechaDesde) {
        return citaRepository.findByEspecialidadAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(especialidad, fechaDesde);
    }

    public List<Cita> listarPorFecha(LocalDate fecha) {
        return citaRepository.findByFechaOrderByHoraAsc(fecha);
    }

    public Cita agendarCita(Cita cita) {
        // La transacción se ejecuta dentro del candado para que el COMMIT ocurra
        // antes de liberar el bloqueo (y el siguiente envío ya vea la cita guardada).
        synchronized (candadoAgenda) {
            return new TransactionTemplate(transactionManager).execute(status -> guardarCitaValidada(cita));
        }
    }

    private Cita guardarCitaValidada(Cita cita) {
        // Los paneles no se mezclan: el paciente debe estar registrado en el área de la especialidad
        AreaClinica areaCita = AreaClinica.paraEspecialidad(cita.getEspecialidad());
        if (areaCita != null && cita.getPaciente() != null
                && cita.getPaciente().getArea() != null
                && cita.getPaciente().getArea() != areaCita) {
            throw new IllegalStateException("El paciente está registrado en el área de "
                    + cita.getPaciente().getArea().getEtiqueta() + ". Regístrelo también en "
                    + areaCita.getEtiqueta() + " para atenderlo en esa especialidad.");
        }

        // Regla de Negocio: Validar que no exista choque de citas en la misma fecha y hora con el mismo médico
        Long idMedico = cita.getMedico().getIdUsuario();
        boolean horarioOcupado = (cita.getIdCita() == null)
                ? citaRepository.existsByMedicoIdUsuarioAndFechaAndHoraAndEstadoNot(
                        idMedico, cita.getFecha(), cita.getHora(), "CANCELADA")
                : citaRepository.existsByMedicoIdUsuarioAndFechaAndHoraAndEstadoNotAndIdCitaNot(
                        idMedico, cita.getFecha(), cita.getHora(), "CANCELADA", cita.getIdCita());

        if (horarioOcupado) {
            throw new IllegalStateException(MENSAJE_HORARIO_OCUPADO);
        }

        cita.setEstado("PROGRAMADA");
        return citaRepository.save(cita);
    }

    @Transactional
    public Cita reprogramarCita(Long idCita, LocalDate nuevaFecha, java.time.LocalTime nuevaHora) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada con ID: " + idCita));

        boolean horarioOcupado = citaRepository.existsByMedicoIdUsuarioAndFechaAndHoraAndEstadoNotAndIdCitaNot(
                cita.getMedico().getIdUsuario(),
                nuevaFecha,
                nuevaHora,
                "CANCELADA",
                cita.getIdCita()
        );

        if (horarioOcupado) {
            throw new IllegalStateException(MENSAJE_HORARIO_OCUPADO);
        }

        cita.setFecha(nuevaFecha);
        cita.setHora(nuevaHora);
        cita.setEstado("PROGRAMADA");
        return citaRepository.save(cita);
    }

    // Marca la cita como NO_ASISTIO sin pasar por agendarCita (que fuerza estado PROGRAMADA)
    @Transactional
    public Cita marcarNoAsistio(Long idCita) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada con ID: " + idCita));
        cita.setEstado("NO_ASISTIO");
        return citaRepository.save(cita);
    }

    @Transactional
    public void cancelarCita(Long idCita, String motivoCancelacion) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada con ID: " + idCita));
        cita.setEstado("CANCELADA");
        cita.setMotivoCancelacion(motivoCancelacion);
        citaRepository.save(cita);
    }

    // CU05: Bloqueos y Horarios
    @Transactional
    public DisponibilidadHorario bloquearHorario(DisponibilidadHorario bloqueo) {
        bloqueo.setEstado("BLOQUEADO");
        return disponibilidadRepository.save(bloqueo);
    }

    public List<DisponibilidadHorario> listarDisponibilidadPorMedicoYFecha(Long idMedico, LocalDate fecha) {
        return disponibilidadRepository.findByMedicoIdUsuarioAndFecha(idMedico, fecha);
    }
}