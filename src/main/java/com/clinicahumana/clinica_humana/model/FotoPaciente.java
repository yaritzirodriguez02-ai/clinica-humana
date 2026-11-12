package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Foto clínica del paciente: placas, radiografías, estudios o retrato.
 *
 * Se guarda en la base de datos como data URI (igual que las firmas y los
 * dibujos de las consultas) para que no se pierda al mover el proyecto.
 * Una de las fotos puede marcarse como foto de perfil del expediente.
 */
@Entity
@Table(name = "fotos_paciente")
public class FotoPaciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_foto")
    private Long idFoto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "nombre_archivo", length = 150)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", length = 80)
    private String tipoContenido;

    /** Imagen completa como data URI (data:image/jpeg;base64,...). */
    @Lob
    @Column(name = "datos_base64", columnDefinition = "LONGTEXT", nullable = false)
    private String datosBase64;

    @Column(name = "es_perfil")
    private Boolean perfil = Boolean.FALSE;

    @Column(name = "fecha_subida", updatable = false)
    private LocalDateTime fechaSubida;

    @PrePersist
    protected void onCreate() {
        this.fechaSubida = LocalDateTime.now();
        if (this.perfil == null) {
            this.perfil = Boolean.FALSE;
        }
    }

    public FotoPaciente() {
    }

    public Long getIdFoto() { return idFoto; }
    public void setIdFoto(Long idFoto) { this.idFoto = idFoto; }

    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getTipoContenido() { return tipoContenido; }
    public void setTipoContenido(String tipoContenido) { this.tipoContenido = tipoContenido; }

    public String getDatosBase64() { return datosBase64; }
    public void setDatosBase64(String datosBase64) { this.datosBase64 = datosBase64; }

    public Boolean getPerfil() { return perfil; }
    public void setPerfil(Boolean perfil) { this.perfil = perfil; }

    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }
}
