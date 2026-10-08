package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.DisponibilidadHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DisponibilidadHorarioRepository extends JpaRepository<DisponibilidadHorario, Long> {
    List<DisponibilidadHorario> findByMedicoIdUsuarioAndFecha(Long idUsuarioMedico, LocalDate fecha);
    List<DisponibilidadHorario> findByFechaAndEstado(LocalDate fecha, String estado);
}