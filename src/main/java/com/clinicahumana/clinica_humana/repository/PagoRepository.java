package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByCitaIdCita(Long idCita);
    List<Pago> findByMedicoIdUsuario(Long idUsuarioMedico);
    List<Pago> findByPacienteIdPaciente(Long idPaciente);
}