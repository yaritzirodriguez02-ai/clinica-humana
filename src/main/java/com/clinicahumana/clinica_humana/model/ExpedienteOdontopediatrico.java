package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expedientes_odontopediatricos")
public class ExpedienteOdontopediatrico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_expediente_odonto")
    private Long idExpedienteOdonto;

    @NotNull(message = "La cita asociada es obligatoria")
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cita", nullable = false, unique = true)
    private Cita cita;

    @NotNull(message = "El paciente es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @NotNull(message = "La especialista odontopediatra es obligatoria")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_medico", nullable = false)
    private Usuario medico;

    @Column(name = "fecha_atencion", updatable = false)
    private LocalDateTime fechaAtencion;

    @NotBlank(message = "El diagnóstico dental es obligatorio")
    @Lob
    @Column(name = "diagnostico_dental", columnDefinition = "TEXT", nullable = false)
    private String diagnosticoDental;

    @NotBlank(message = "Los procedimientos realizados son obligatorios")
    @Lob
    @Column(name = "procedimientos_realizados", columnDefinition = "TEXT", nullable = false)
    private String procedimientosRealizados;

    @Column(name = "piezas_tratadas", length = 100)
    private String piezasTratadas;

    @Lob
    @Column(name = "recomendaciones_higiene", columnDefinition = "TEXT")
    private String recomendacionesHigiene;

    @NotNull(message = "El costo del tratamiento es obligatorio")
    @DecimalMin(value = "0.00", message = "El costo del tratamiento no puede ser negativo")
    @Column(name = "costo_tratamiento", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoTratamiento;

    @Lob
    @Column(name = "pizarron_dental_base64", columnDefinition = "LONGTEXT")
    private String pizarronDentalBase64;

    @NotBlank(message = "La firma médica es obligatoria")
    @Lob
    @Column(name = "firma_medico_base64", columnDefinition = "LONGTEXT", nullable = false)
    private String firmaMedicoBase64;

    @PrePersist
    protected void onCreate() {
        this.fechaAtencion = LocalDateTime.now();
    }

    public ExpedienteOdontopediatrico() {
    }

    public Long getIdExpedienteOdonto() {
        return idExpedienteOdonto;
    }

    public void setIdExpedienteOdonto(Long idExpedienteOdonto) {
        this.idExpedienteOdonto = idExpedienteOdonto;
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

    public String getDiagnosticoDental() {
        return diagnosticoDental;
    }

    public void setDiagnosticoDental(String diagnosticoDental) {
        this.diagnosticoDental = diagnosticoDental;
    }

    public String getProcedimientosRealizados() {
        return procedimientosRealizados;
    }

    public void setProcedimientosRealizados(String procedimientosRealizados) {
        this.procedimientosRealizados = procedimientosRealizados;
    }

    public String getPiezasTratadas() {
        return piezasTratadas;
    }

    public void setPiezasTratadas(String piezasTratadas) {
        this.piezasTratadas = piezasTratadas;
    }

    public String getRecomendacionesHigiene() {
        return recomendacionesHigiene;
    }

    public void setRecomendacionesHigiene(String recomendacionesHigiene) {
        this.recomendacionesHigiene = recomendacionesHigiene;
    }

    public BigDecimal getCostoTratamiento() {
        return costoTratamiento;
    }

    public void setCostoTratamiento(BigDecimal costoTratamiento) {
        this.costoTratamiento = costoTratamiento;
    }

    public String getPizarronDentalBase64() {
        return pizarronDentalBase64;
    }

    public void setPizarronDentalBase64(String pizarronDentalBase64) {
        this.pizarronDentalBase64 = pizarronDentalBase64;
    }

    public String getFirmaMedicoBase64() {
        return firmaMedicoBase64;
    }

    public void setFirmaMedicoBase64(String firmaMedicoBase64) {
        this.firmaMedicoBase64 = firmaMedicoBase64;
    }
}