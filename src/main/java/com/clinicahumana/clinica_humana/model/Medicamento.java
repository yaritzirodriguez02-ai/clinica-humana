package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Catálogo de fármacos (Vademécum interno) usado por el módulo de recetas.
 * Permite autocompletar presentación y concentración al capturar la receta.
 */
@Entity
@Table(name = "medicamentos")
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medicamento")
    private Long idMedicamento;

    @NotBlank(message = "El nombre comercial es obligatorio")
    @Size(max = 120)
    @Column(name = "nombre_comercial", nullable = false, length = 120)
    private String nombreComercial;

    @NotBlank(message = "La sustancia activa es obligatoria")
    @Size(max = 120)
    @Column(name = "sustancia_activa", nullable = false, length = 120)
    private String sustanciaActiva;

    @Size(max = 80)
    @Column(name = "presentacion", length = 80)
    private String presentacion;

    @Size(max = 60)
    @Column(name = "concentracion", length = 60)
    private String concentracion;

    @NotBlank(message = "La categoría es obligatoria")
    @Column(name = "categoria", nullable = false, length = 20)
    private String categoria = "GENERAL"; // 'GENERAL', 'PEDIATRIA', 'ODONTOLOGIA'

    public Medicamento() {
    }

    public Long getIdMedicamento() { return idMedicamento; }
    public void setIdMedicamento(Long idMedicamento) { this.idMedicamento = idMedicamento; }

    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }

    public String getSustanciaActiva() { return sustanciaActiva; }
    public void setSustanciaActiva(String sustanciaActiva) { this.sustanciaActiva = sustanciaActiva; }

    public String getPresentacion() { return presentacion; }
    public void setPresentacion(String presentacion) { this.presentacion = presentacion; }

    public String getConcentracion() { return concentracion; }
    public void setConcentracion(String concentracion) { this.concentracion = concentracion; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}
