package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bitácora de los mensajes (SMS / WhatsApp) enviados al tutor o paciente por una cita.
 * Sirve para no repetir el recordatorio del día y para poder auditar qué se envió,
 * a qué número y con qué resultado (enviado, simulado o error del proveedor).
 */
@Entity
@Table(name = "notificaciones_cita")
public class NotificacionCita {

    public static final String CANAL_SMS = "SMS";
    public static final String CANAL_WHATSAPP = "WHATSAPP";

    public static final String TIPO_CONFIRMACION = "CONFIRMACION";
    public static final String TIPO_RECORDATORIO = "RECORDATORIO";

    public static final String ESTADO_ENVIADO = "ENVIADO";
    public static final String ESTADO_SIMULADO = "SIMULADO";
    public static final String ESTADO_ERROR = "ERROR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cita", nullable = false)
    private Cita cita;

    @Column(name = "canal", nullable = false, length = 20)
    private String canal;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "mensaje", length = 1000)
    private String mensaje;

    @Column(name = "estado", length = 20)
    private String estado;

    @Column(name = "detalle_error", length = 255)
    private String detalleError;

    @Column(name = "fecha_envio", updatable = false)
    private LocalDateTime fechaEnvio;

    @PrePersist
    protected void onCreate() {
        if (this.fechaEnvio == null) {
            this.fechaEnvio = LocalDateTime.now();
        }
    }

    public Long getIdNotificacion() {
        return idNotificacion;
    }

    public void setIdNotificacion(Long idNotificacion) {
        this.idNotificacion = idNotificacion;
    }

    public Cita getCita() {
        return cita;
    }

    public void setCita(Cita cita) {
        this.cita = cita;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getDetalleError() {
        return detalleError;
    }

    public void setDetalleError(String detalleError) {
        this.detalleError = detalleError;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}
