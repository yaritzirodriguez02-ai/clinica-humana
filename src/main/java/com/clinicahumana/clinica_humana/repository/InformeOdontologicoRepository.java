package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.InformeOdontologico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InformeOdontologicoRepository extends JpaRepository<InformeOdontologico, Long> {

    List<InformeOdontologico> findAllByOrderByFechaEmisionDescIdInformeDesc();

    List<InformeOdontologico> findByPacienteIdPacienteOrderByFechaEmisionDescIdInformeDesc(Long idPaciente);
}
