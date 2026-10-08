package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {
    // Para evitar citas duplicadas en el mismo horario con el mismo especialista
    boolean existsByMedicoIdUsuarioAndFechaAndHoraAndEstadoNot(Long idUsuarioMedico, LocalDate fecha, LocalTime hora, String estado);

    List<Cita> findByFechaAndEspecialidad(LocalDate fecha, String especialidad);
    List<Cita> findByMedicoIdUsuarioAndFecha(Long idUsuarioMedico, LocalDate fecha);
    List<Cita> findByPacienteIdPaciente(Long idPaciente);
    List<Cita> findByEstado(String estado);
}