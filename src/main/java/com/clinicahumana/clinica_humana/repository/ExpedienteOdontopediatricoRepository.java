package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.ExpedienteOdontopediatrico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedienteOdontopediatricoRepository extends JpaRepository<ExpedienteOdontopediatrico, Long> {
    Optional<ExpedienteOdontopediatrico> findByCitaIdCita(Long idCita);

    List<ExpedienteOdontopediatrico> findByPacienteIdPacienteOrderByFechaAtencionDesc(Long idPaciente);

    List<ExpedienteOdontopediatrico> findByPacienteIdPacienteAndMedicoIdUsuarioOrderByFechaAtencionDesc(Long idPaciente, Long idMedico);

    boolean existsByPacienteIdPacienteAndIdExpedienteOdonto(Long idPaciente, Long idExpedienteOdonto);
}