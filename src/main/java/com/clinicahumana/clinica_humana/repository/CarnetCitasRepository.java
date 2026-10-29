package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.CarnetCitas;
import com.clinicahumana.clinica_humana.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarnetCitasRepository extends JpaRepository<CarnetCitas, Long> {

    /**
     * Carnet por idCarnet (campo único).
     */
    Optional<CarnetCitas> findByIdCarnet(Long idCarnet);

    /**
     * Todos los carnets asociados a un paciente.
     */
    List<CarnetCitas> findByPaciente(Paciente paciente);

    /**
     * Todos los carnets asociados a un paciente (busqueda por idPaciente).
     */
    List<CarnetCitas> findByPacienteIdPaciente(Long idPaciente);

    /**
     * Cuenta de carnets por paciente.
     */
    long countByPacienteIdPaciente(Long idPaciente);

    // Depuración de registros
    List<CarnetCitas> findByPacienteIdPacienteIn(java.util.Collection<Long> idsPaciente);
}
