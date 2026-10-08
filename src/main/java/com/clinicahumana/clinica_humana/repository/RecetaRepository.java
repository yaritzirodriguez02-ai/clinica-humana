package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {
    List<Receta> findByCitaIdCitaOrderByIdRecetaDesc(Long idCita);
    List<Receta> findByPacienteIdPacienteOrderByFechaEmisionDesc(Long idPaciente);
}