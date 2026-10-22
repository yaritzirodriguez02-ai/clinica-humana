package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Carnet de Citas / Carnet de Atención del módulo CISO
 * (Clínica Integral de Servicios Odontológicos).
 *
 * Registro ligero del carnet del paciente vinculado al tratamiento
 * programado y el total a pagar.
 */
@Entity
@Table(name = "carnet_citas")
public class CarnetCitas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carnet", unique = true, nullable = false)
    private Long idCarnet;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    /** Fecha impresa en el carnet (puede diferir de la cita si se reagenda). */
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /** Hora impresa en el carnet. */
    @Column(name = "hora")
    private LocalTime hora;

    /** Tratamiento que se va a realizar (texto libre del operador). */
    @Column(name = "tratamiento_realizar", length = 255)
    private String tratamientoRealizar;

    /** Total a pagar registrado en el carnet (en moneda local, dos decimales). */
    @Column(name = "total_pago", precision = 10, scale = 2)
    private java.math.BigDecimal totalPago;

    public CarnetCitas() {
    }

    public CarnetCitas(Paciente paciente, LocalDate fecha, LocalTime hora,
                       String tratamientoRealizar, java.math.BigDecimal totalPago) {
        this.paciente = paciente;
        this.fecha = fecha;
        this.hora = hora;
        this.tratamientoRealizar = tratamientoRealizar;
        this.totalPago = totalPago;
    }

    public Long getIdCarnet() {
        return idCarnet;
    }

    public void setIdCarnet(Long idCarnet) {
        this.idCarnet = idCarnet;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
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

    public String getTratamientoRealizar() {
        return tratamientoRealizar;
    }

    public void setTratamientoRealizar(String tratamientoRealizar) {
        this.tratamientoRealizar = tratamientoRealizar;
    }

    public java.math.BigDecimal getTotalPago() {
        return totalPago;
    }

    public void setTotalPago(java.math.BigDecimal totalPago) {
        this.totalPago = totalPago;
    }
}
