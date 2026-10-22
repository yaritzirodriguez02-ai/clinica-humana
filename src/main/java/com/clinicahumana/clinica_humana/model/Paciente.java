package com.clinicahumana.clinica_humana.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(name = "pacientes")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paciente")
    private Long idPaciente;

    // Llave foránea hacia la tabla tutores
    /**
     * Tutor legal del paciente.
     * Dejado opcional para permitir el registro de pacientes adultos de forma independiente.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tutor")
    private Tutor tutor;

    @NotBlank(message = "El nombre del paciente es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,100}$", message = "El nombre solo debe contener letras y espacios (sin números ni símbolos)")
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,100}$", message = "Los apellidos solo deben contener letras y espacios (sin números ni símbolos)")
    @Size(max = 100)
    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    /**
     * CURP (Código Único de Registro de Población).
     * Identificador oficial del paciente (dejado opcional para permitir registro de adultos sin CURP).
     */
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9A-Z]{2}$", message = "La CURP no cumple con el formato estándar oficial de 18 caracteres")
    @Column(name = "curp", unique = true, length = 18)
    private String curp;

    /**
     * Fecha de nacimiento.
     * Si el paciente es adulto registrado de forma independiente, este campo puede ir vacío.
     */
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    /**
     * Sexo del paciente: 'M' o 'F'.
     * Dejado opcional para permitir registro de adultos de forma independiente.
     */
    @Column(name = "sexo", length = 1)
    private String sexo; // 'M' o 'F'

    @Column(name = "tipo_sangre", length = 5)
    private String tipoSangre;

    @Lob
    @Column(name = "alergias", columnDefinition = "TEXT")
    private String alergias;

    @Lob
    @Column(name = "antecedentes", columnDefinition = "TEXT")
    private String antecedentes;

    @Column(name = "foto_ruta")
    private String fotoRuta;

    public Paciente() {
    }

    // Getters y Setters
    public Long getIdPaciente() {
        return idPaciente;
    }

    public void setIdPaciente(Long idPaciente) {
        this.idPaciente = idPaciente;
    }

    public Tutor getTutor() {
        return tutor;
    }

    public void setTutor(Tutor tutor) {
        this.tutor = tutor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCurp() {
        return curp;
    }

    public void setCurp(String curp) {
        this.curp = curp;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getTipoSangre() {
        return tipoSangre;
    }

    public void setTipoSangre(String tipoSangre) {
        this.tipoSangre = tipoSangre;
    }

    public String getAlergias() {
        return alergias;
    }

    public void setAlergias(String alergias) {
        this.alergias = alergias;
    }

    public String getAntecedentes() {
        return antecedentes;
    }

    public void setAntecedentes(String antecedentes) {
        this.antecedentes = antecedentes;
    }

    public String getFotoRuta() {
        return fotoRuta;
    }

    public void setFotoRuta(String fotoRuta) {
        this.fotoRuta = fotoRuta;
    }
}