package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Long idCita;

    @NotNull(message = "El paciente es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @NotNull(message = "El médico especialista es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_medico", nullable = false)
    private Usuario medico;

    @NotBlank(message = "La especialidad es obligatoria")
    @Column(name = "especialidad", nullable = false, length = 30)
    private String especialidad; // 'PEDIATRIA' o 'ODONTOPEDIATRIA'

    @NotNull(message = "La fecha de la cita es obligatoria")
    @FutureOrPresent(message = "La fecha de la cita debe ser a partir del día de hoy en adelante")
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @NotNull(message = "La hora de la cita es obligatoria")
    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @NotBlank(message = "El motivo de la cita es obligatorio")
    @Size(min = 10, max = 255, message = "El motivo de la cita debe ser detallado (entre 10 y 255 caracteres)")
    @Column(name = "motivo", nullable = false, length = 255)
    private String motivo;

    @Column(name = "estado", length = 30)
    private String estado = "PROGRAMADA"; // 'PROGRAMADA', 'EN_CONSULTA', 'COMPLETADA', 'CANCELADA', 'NO_ASISTIO'

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    @Lob
    @Column(name = "firma_tutor_base64", columnDefinition = "LONGTEXT")
    private String firmaTutorBase64;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public Cita() {
    }

    public Long getIdCita() {
        return idCita;
    }

    public void setIdCita(Long idCita) {
        this.idCita = idCita;
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

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public String getFirmaTutorBase64() {
        return firmaTutorBase64;
    }

    public void setFirmaTutorBase64(String firmaTutorBase64) {
        this.firmaTutorBase64 = firmaTutorBase64;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}