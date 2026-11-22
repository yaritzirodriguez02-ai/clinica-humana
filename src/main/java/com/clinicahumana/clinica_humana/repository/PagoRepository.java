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

    List<Pago> findByFechaPagoBetween(java.time.LocalDateTime inicio, java.time.LocalDateTime fin);

    // Filtrado por área/especialidad (para separar paneles de cobro)
    List<Pago> findByCitaEspecialidad(String especialidad);
    List<Pago> findByCitaEspecialidadIn(java.util.Collection<String> especialidades);

    // Depuración de registros
    List<Pago> findByCitaIdCitaIn(java.util.Collection<Long> idsCita);

    List<Pago> findByPacienteIdPacienteIn(java.util.Collection<Long> idsPaciente);
}