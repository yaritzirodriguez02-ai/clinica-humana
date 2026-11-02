package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.AreaClinica;
import com.clinicahumana.clinica_humana.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    // La CURP se repite entre áreas: la misma persona puede estar registrada en odontología y en pediatría
    Optional<Paciente> findByCurpAndArea(String curp, AreaClinica area);
    boolean existsByCurpAndArea(String curp, AreaClinica area);

    // Directorio separado por área clínica
    List<Paciente> findByArea(AreaClinica area);

    List<Paciente> findByAreaAndNombreContainingIgnoreCaseOrAreaAndApellidosContainingIgnoreCase(
            AreaClinica areaNombre, String nombre, AreaClinica areaApellidos, String apellidos);

    // Depuración de registros: tutor sin pacientes pendientes
    boolean existsByTutorIdTutor(Long idTutor);
}