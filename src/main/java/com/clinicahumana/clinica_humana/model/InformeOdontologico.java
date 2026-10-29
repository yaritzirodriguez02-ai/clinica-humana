package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Informe clínico que la odontóloga redacta sobre una consulta y que se imprime
 * en la hoja membretada oficial de CISO (tamaño carta).
 *
 * No sustituye al expediente: lo complementa con la explicación amplia del
 * diagnóstico, el resumen del tratamiento o cualquier documento que la clínica
 * necesite entregar al paciente/tutor.
 */
@Entity
@Table(name = "informes_odontologicos")
public class InformeOdontologico {

    /** Tipos de documento disponibles y su etiqueta legible. */
    public static final Map<String, String> TIPOS = new LinkedHashMap<>();
    static {
        TIPOS.put("INFORME_CLINICO", "Informe clínico");
        TIPOS.put("EXPLICACION_DIAGNOSTICO", "Explicación del diagnóstico");
        TIPOS.put("INFORME_TRATAMIENTO", "Informe de tratamiento");
        TIPOS.put("CONSTANCIA_ATENCION", "Constancia de atención");
        TIPOS.put("JUSTIFICANTE", "Justificante");
        TIPOS.put("OTRO", "Otro documento");
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_informe")
    private Long idInforme;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    /** Consulta de la que se deriva el informe (opcional). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cita")
    private Cita cita;

    /** Profesional que suscribe el documento. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario_medico", nullable = false)
    private Usuario medico;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision = LocalDate.now();

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo = "INFORME_CLINICO";

    /** Sede que aparece en la línea de lugar y fecha. */
    @Column(name = "lugar", length = 80)
    private String lugar = "Chilpancingo, Gro.";

    @Column(name = "destinatario", length = 150)
    private String destinatario;

    @NotBlank(message = "El asunto del informe es obligatorio")
    @Size(max = 200, message = "El asunto no debe exceder 200 caracteres")
    @Column(name = "asunto", nullable = false, length = 200)
    private String asunto;

    @NotBlank(message = "El cuerpo del informe es obligatorio")
    @Lob
    @Column(name = "contenido", columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public InformeOdontologico() {
    }

    /** Folio oficial del documento: CISO-INF-0001/2026. */
    public String getFolio() {
        if (idInforme == null) {
            return "CISO-INF-BORRADOR";
        }
        return String.format("CISO-INF-%04d/%d", idInforme, fechaEmision != null ? fechaEmision.getYear() : LocalDate.now().getYear());
    }

    public String getTipoEtiqueta() {
        return TIPOS.getOrDefault(tipo, "Informe clínico");
    }

    /** Nombre del suscriptor sin el cargo pegado al apellido ("... - CIRUJANA DENTISTA"). */
    public String getNombreSuscriptor() {
        if (medico == null || medico.getNombreCompleto() == null) {
            return "DRA. GLORIA GONZÁLEZ HERRERA";
        }
        String nombre = medico.getNombreCompleto();
        return nombre.contains(" - ") ? nombre.substring(0, nombre.indexOf(" - ")).trim() : nombre.trim();
    }

    public Long getIdInforme() { return idInforme; }
    public void setIdInforme(Long idInforme) { this.idInforme = idInforme; }

    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }

    public Cita getCita() { return cita; }
    public void setCita(Cita cita) { this.cita = cita; }

    public Usuario getMedico() { return medico; }
    public void setMedico(Usuario medico) { this.medico = medico; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
