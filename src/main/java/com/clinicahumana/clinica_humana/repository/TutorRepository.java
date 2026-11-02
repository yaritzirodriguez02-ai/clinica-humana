package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.AreaClinica;
import com.clinicahumana.clinica_humana.model.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {
    // Los tutores tampoco se comparten entre áreas: se buscan sólo dentro del área donde se registraron
    List<Tutor> findByArea(AreaClinica area);
    List<Tutor> findByTelefonoAndArea(String telefono, AreaClinica area);
    List<Tutor> findByApellidosContainingIgnoreCase(String apellidos);
}