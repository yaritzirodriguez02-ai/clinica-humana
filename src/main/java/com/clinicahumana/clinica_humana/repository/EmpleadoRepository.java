package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByUsuarioIdUsuario(Long idUsuario);
    Optional<Empleado> findByCargo(String cargo);
}