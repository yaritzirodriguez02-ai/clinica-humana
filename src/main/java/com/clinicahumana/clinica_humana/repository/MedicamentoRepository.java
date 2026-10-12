package com.clinicahumana.clinica_humana.repository;

import com.clinicahumana.clinica_humana.model.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {
    List<Medicamento> findAllByOrderByNombreComercialAsc();
    List<Medicamento> findByCategoriaInOrderByNombreComercialAsc(List<String> categorias);
    List<Medicamento> findByNombreComercialContainingIgnoreCaseOrderByNombreComercialAsc(String nombre);
    Optional<Medicamento> findByNombreComercialIgnoreCase(String nombreComercial);
}
