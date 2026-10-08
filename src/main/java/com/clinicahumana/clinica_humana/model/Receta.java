package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recetas")
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_receta")
    private Long idReceta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_cita", nullable = false)
    private Cita cita;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_medico", nullable = false)
    private Usuario medico;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision = LocalDate.now();

    @Column(name = "indicaciones_generales", columnDefinition = "TEXT")
    private String indicacionesGenerales;

    @Column(name = "diagnostico_resumen", length = 255)
    private String diagnosticoResumen;

    @Column(name = "firma_base64", columnDefinition = "LONGTEXT")
    private String firmaBase64;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
private List<RecetaDetalle> detalles = new ArrayList<>();

    public Receta() {}

    public void agregarDetalle(RecetaDetalle detalle) {
        detalles.add(detalle);
        detalle.setReceta(this);
    }

    public Long getIdReceta() { return idReceta; }
    public void setIdReceta(Long idReceta) { this.idReceta = idReceta; }

    public Cita getCita() { return cita; }
    public void setCita(Cita cita) { this.cita = cita; }

    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }

    public Usuario getMedico() { return medico; }
    public void setMedico(Usuario medico) { this.medico = medico; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getIndicacionesGenerales() { return indicacionesGenerales; }
    public void setIndicacionesGenerales(String indicacionesGenerales) { this.indicacionesGenerales = indicacionesGenerales; }

    public String getDiagnosticoResumen() { return diagnosticoResumen; }
    public void setDiagnosticoResumen(String diagnosticoResumen) { this.diagnosticoResumen = diagnosticoResumen; }

    public String getFirmaBase64() { return firmaBase64; }
    public void setFirmaBase64(String firmaBase64) { this.firmaBase64 = firmaBase64; }

    public List<RecetaDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<RecetaDetalle> detalles) { this.detalles = detalles; }
}