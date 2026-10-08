package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.ExpedientePediatrico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedientePediatricoRepository extends JpaRepository<ExpedientePediatrico, Long> {
    Optional<ExpedientePediatrico> findByCitaIdCita(Long idCita);
    List<ExpedientePediatrico> findByPacienteIdPacienteOrderByFechaAtencionDesc(Long idPaciente);
}