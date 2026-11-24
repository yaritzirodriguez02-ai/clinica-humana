package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "receta_detalles")
public class RecetaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_receta", nullable = false)
    private Receta receta;

    // Prescripción libre: se acepta cualquier medicamento, venga del catálogo o se escriba a mano.
    @Size(max = 120, message = "El medicamento no debe exceder 120 caracteres")
    @Column(nullable = false, length = 120)
    private String medicamento;

    @Column(length = 80)
    private String presentacion;

    // Dosis, frecuencia y duración en texto libre: sin formatos ni catálogos obligatorios.
    @Size(max = 60, message = "La dosis no debe exceder 60 caracteres")
    @Column(nullable = false, length = 60)
    private String dosis;

    @Size(max = 60, message = "La frecuencia no debe exceder 60 caracteres")
    @Column(nullable = false, length = 60)
    private String frecuencia;

    @Size(max = 40, message = "La duración no debe exceder 40 caracteres")
    @Column(nullable = false, length = 40)
    private String duracion;

    @Column(name = "indicaciones_especificas", length = 200)
    private String indicacionesEspecificas;

    public RecetaDetalle() {}

    public Long getIdDetalle() { return idDetalle; }
    public void setIdDetalle(Long idDetalle) { this.idDetalle = idDetalle; }

    public Receta getReceta() { return receta; }
    public void setReceta(Receta receta) { this.receta = receta; }

    public String getMedicamento() { return medicamento; }
    public void setMedicamento(String medicamento) { this.medicamento = medicamento; }

    public String getPresentacion() { return presentacion; }
    public void setPresentacion(String presentacion) { this.presentacion = presentacion; }

    public String getDosis() { return dosis; }
    public void setDosis(String dosis) { this.dosis = dosis; }

    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }

    public String getDuracion() { return duracion; }
    public void setDuracion(String duracion) { this.duracion = duracion; }

    public String getIndicacionesEspecificas() { return indicacionesEspecificas; }
    public void setIndicacionesEspecificas(String indicacionesEspecificas) { this.indicacionesEspecificas = indicacionesEspecificas; }
}