package com.clinicahumana.clinica_humana.service;

import com.clinicahumana.clinica_humana.model.Cita;
import com.clinicahumana.clinica_humana.model.NotificacionCita;
import com.clinicahumana.clinica_humana.repository.CitaRepository;
import com.clinicahumana.clinica_humana.repository.NotificacionCitaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Envía cada mañana el recordatorio de las citas del día por SMS y WhatsApp,
 * indicando la hora de llegada (20 minutos antes), la doctora asignada y los
 * datos registrados. Si un recordatorio ya se envió, no se repite.
 */
@Service
public class RecordatorioCitasService {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioCitasService.class);

    private static final Set<String> ESTADOS_SIN_RECORDATORIO =
            Set.of("CANCELADA", "COMPLETADA", "NO_ASISTIO");

    @Autowired
    private CitaRepository citaRepo;

    @Autowired
    private NotificacionCitaRepository notificacionRepo;

    @Autowired
    private NotificacionCitaService notificaciones;

    @Scheduled(cron = "${notificaciones.recordatorio.cron:0 0 7 * * *}",
            zone = "${notificaciones.recordatorio.zona:America/Mexico_City}")
    public void enviarRecordatoriosDelDia() {
        log.info("Enviando recordatorios de citas del día {}", LocalDate.now());
        enviarRecordatorios(LocalDate.now(), LocalTime.now());
    }

    /** Separado del @Scheduled para poder probarlo con una fecha/hora concreta. */
    public int enviarRecordatorios(LocalDate fecha, LocalTime ahora) {
        List<Cita> citas = citaRepo.findByFechaOrderByHoraAsc(fecha);
        int enviados = 0;

        for (Cita cita : citas) {
            if (cita.getEstado() != null && ESTADOS_SIN_RECORDATORIO.contains(cita.getEstado().toUpperCase())) {
                continue;
            }
            if (cita.getHora() != null && !cita.getHora().isAfter(ahora)) {
                continue; // la cita ya pasó o está en curso
            }
            if (notificacionRepo.existsByCitaIdCitaAndTipoAndEstadoNot(
                    cita.getIdCita(), NotificacionCita.TIPO_RECORDATORIO, NotificacionCita.ESTADO_ERROR)) {
                continue;
            }
            try {
                String resumen = notificaciones.recordar(cita);
                if (!resumen.isEmpty()) {
                    enviados++;
                }
            } catch (Exception e) {
                log.warn("No se pudo enviar el recordatorio de la cita #{}: {}",
                        cita.getIdCita(), e.getMessage());
            }
        }

        log.info("Recordatorios enviados: {} de {} citas del día", enviados, citas.size());
        return enviados;
    }
}
