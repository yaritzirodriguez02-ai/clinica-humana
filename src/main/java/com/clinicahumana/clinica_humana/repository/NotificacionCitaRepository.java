package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.NotificacionCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionCitaRepository extends JpaRepository<NotificacionCita, Long> {

    List<NotificacionCita> findByCitaIdCita(Long idCita);

    boolean existsByCitaIdCitaAndTipoAndEstadoNot(Long idCita, String tipo, String estado);
}
