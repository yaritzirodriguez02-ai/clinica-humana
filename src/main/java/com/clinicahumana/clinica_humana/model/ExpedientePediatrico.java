package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expedientes_pediatricos")
public class ExpedientePediatrico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_expediente_ped")
    private Long idExpedientePed;

    @NotNull(message = "La cita asociada es obligatoria")
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cita", nullable = false, unique = true)
    private Cita cita;

    @NotNull(message = "El paciente es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @NotNull(message = "El médico pediatra es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_medico", nullable = false)
    private Usuario medico;

    @Column(name = "fecha_atencion", updatable = false)
    private LocalDateTime fechaAtencion;

    // Somatometría con validación de rangos
    @NotNull(message = "El peso es obligatorio")
    @DecimalMin(value = "1.00", message = "El peso mínimo permitido es 1.00 kg")
    @DecimalMax(value = "100.00", message = "El peso máximo permitido es 100.00 kg")
    @Column(name = "peso", nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    @NotNull(message = "La talla es obligatoria")
    @DecimalMin(value = "40.00", message = "La talla mínima permitida es 40.00 cm")
    @DecimalMax(value = "200.00", message = "La talla máxima permitida es 200.00 cm")
    @Column(name = "talla", nullable = false, precision = 5, scale = 2)
    private BigDecimal talla;

    @NotNull(message = "La temperatura es obligatoria")
    @DecimalMin(value = "35.00", message = "La temperatura mínima permitida es 35.00 °C")
    @DecimalMax(value = "42.00", message = "La temperatura máxima permitida es 42.00 °C")
    @Column(name = "temperatura", nullable = false, precision = 4, scale = 2)
    private BigDecimal temperatura;

    @Min(value = 40, message = "La frecuencia cardíaca pediátrica mínima es de 40 lpm")
    @Max(value = 220, message = "La frecuencia cardíaca pediátrica no puede superar los 220 lpm")
    @Column(name = "frecuencia_cardiaca")
    private Integer frecuenciaCardiaca;

    @Pattern(regexp = "^$|^[0-9]{2,3}/[0-9]{2,3}$", message = "La presión arterial debe tener formato sistólica/diastólica (ej. 110/70)")
    @Column(name = "presion_arterial", length = 20)
    private String presionArterial;

    @Min(value = 50, message = "La saturación de oxígeno mínima medible es 50%")
    @Max(value = 100, message = "La saturación de oxígeno no puede exceder el 100%")
    @Column(name = "saturacion_oxigeno")
    private Integer saturacionOxigeno;

    // --- Evaluación clínica obligatoria ---
    @NotBlank(message = "El diagnóstico médico es obligatorio")
    @Size(min = 5, max = 1000, message = "El diagnóstico debe tener entre 5 y 1000 caracteres")
    @Column(name = "diagnostico_medico", columnDefinition = "TEXT", nullable = false)
    private String diagnostico;

    @NotBlank(message = "El tratamiento indicado es obligatorio")
    @Column(name = "tratamiento", columnDefinition = "TEXT")
    private String tratamiento;

    @NotBlank(message = "La receta médica es obligatoria")
    @Lob
    @Column(name = "receta_medica", columnDefinition = "TEXT", nullable = false)
    private String recetaMedica;

    @Lob
    @Column(name = "pizarron_dibujo_base64", columnDefinition = "LONGTEXT")
    private String pizarronDibujoBase64;

    @NotBlank(message = "La firma médica es obligatoria")
    @Lob
    @Column(name = "firma_medico_base64", columnDefinition = "LONGTEXT", nullable = false)
    private String firmaMedicoBase64;

    @PrePersist
    protected void onCreate() {
        this.fechaAtencion = LocalDateTime.now();
    }

    public ExpedientePediatrico() {
    }

    public Long getIdExpedientePed() {
        return idExpedientePed;
    }

    public void setIdExpedientePed(Long idExpedientePed) {
        this.idExpedientePed = idExpedientePed;
    }

    public Cita getCita() {
        return cita;
    }

    public void setCita(Cita cita) {
        this.cita = cita;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Usuario getMedico() {
        return medico;
    }

    public void setMedico(Usuario medico) {
        this.medico = medico;
    }

    public LocalDateTime getFechaAtencion() {
        return fechaAtencion;
    }

    public void setFechaAtencion(LocalDateTime fechaAtencion) {
        this.fechaAtencion = fechaAtencion;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }

    public BigDecimal getTalla() {
        return talla;
    }

    public void setTalla(BigDecimal talla) {
        this.talla = talla;
    }

    public BigDecimal getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(BigDecimal temperatura) {
        this.temperatura = temperatura;
    }

    public Integer getFrecuenciaCardiaca() {
        return frecuenciaCardiaca;
    }

    public void setFrecuenciaCardiaca(Integer frecuenciaCardiaca) {
        this.frecuenciaCardiaca = frecuenciaCardiaca;
    }

    public String getPresionArterial() {
        return presionArterial;
    }

    public void setPresionArterial(String presionArterial) {
        this.presionArterial = presionArterial;
    }

    public Integer getSaturacionOxigeno() {
        return saturacionOxigeno;
    }

    public void setSaturacionOxigeno(Integer saturacionOxigeno) {
        this.saturacionOxigeno = saturacionOxigeno;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public String getRecetaMedica() {
        return recetaMedica;
    }

    public void setRecetaMedica(String recetaMedica) {
        this.recetaMedica = recetaMedica;
    }

    public String getPizarronDibujoBase64() {
        return pizarronDibujoBase64;
    }

    public void setPizarronDibujoBase64(String pizarronDibujoBase64) {
        this.pizarronDibujoBase64 = pizarronDibujoBase64;
    }

    public String getFirmaMedicoBase64() {
        return firmaMedicoBase64;
    }

    public void setFirmaMedicoBase64(String firmaMedicoBase64) {
        this.firmaMedicoBase64 = firmaMedicoBase64;
    }
}