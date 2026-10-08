package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {
    List<Tutor> findByTelefono(String telefono);
    List<Tutor> findByApellidosContainingIgnoreCase(String apellidos);
}