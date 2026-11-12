package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.FotoPaciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FotoPacienteRepository extends JpaRepository<FotoPaciente, Long> {

    List<FotoPaciente> findByPacienteIdPacienteOrderByFechaSubidaDesc(Long idPaciente);

    List<FotoPaciente> findByPacienteIdPacienteInAndPerfilTrue(Collection<Long> idsPaciente);

    Optional<FotoPaciente> findFirstByPacienteIdPacienteAndPerfilTrue(Long idPaciente);
}
