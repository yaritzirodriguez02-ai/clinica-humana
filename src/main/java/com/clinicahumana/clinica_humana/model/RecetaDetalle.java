package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.clinicahumana.clinica_humana.validation.ValidacionesClinicas;

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

    @NotBlank(message = "El medicamento es obligatorio")
    @Pattern(regexp = ValidacionesClinicas.MEDICAMENTO_REGEX, message = "El medicamento debe ser un nombre de fármaco real (letras, números y signos básicos), sin texto aleatorio")
    @Column(nullable = false, length = 120)
    private String medicamento;

    @Column(length = 80)
    private String presentacion;

    @NotBlank(message = "La dosis es obligatoria")
    @Size(max = 60, message = "La dosis no debe exceder 60 caracteres")
    @Column(nullable = false, length = 60)
    private String dosis;

    @NotBlank(message = "La frecuencia es obligatoria")
    @Pattern(regexp = ValidacionesClinicas.FRECUENCIA_REGEX, message = "La frecuencia debe tener formato estructurado (ej. cada 8 horas, 8 hrs, c/8h)")
    @Column(nullable = false, length = 60)
    private String frecuencia;

    @NotBlank(message = "La duración es obligatoria")
    @Pattern(regexp = ValidacionesClinicas.DURACION_REGEX, message = "La duración debe tener formato estructurado (ej. por 5 días)")
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