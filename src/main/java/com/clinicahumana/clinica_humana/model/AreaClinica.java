package com.clinicahumana.clinica_humana.model;

/**
 * Áreas clínicas independientes del sistema.
 *
 * Cada paciente y cada tutor pertenece a una sola área: los registrados en
 * odontología (o en recepción) no aparecen en el panel de pediatría y viceversa.
 * Si una misma persona acude a las dos especialidades se registra una vez en cada área.
 */
public enum AreaClinica {

    ODONTOLOGIA("odontologia", "Odontología"),
    PEDIATRIA("pediatria", "Pediatría");

    private final String slug;
    private final String etiqueta;

    AreaClinica(String slug, String etiqueta) {
        this.slug = slug;
        this.etiqueta = etiqueta;
    }

    public String getSlug() {
        return slug;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Área a la que pertenece el usuario según su rol; null para el administrador (ve ambas). */
    public static AreaClinica paraRol(String rol) {
        if (rol == null) {
            return null;
        }
        return switch (rol.trim().toUpperCase()) {
            case "ODONTOPEDIATRA", "ODONTOLOGIA", "RECEPCIONISTA" -> ODONTOLOGIA;
            case "PEDIATRA" -> PEDIATRIA;
            default -> null;
        };
    }

    /** Área equivalente a una especialidad clínica ('ODONTOLOGIA' / 'PEDIATRIA'). */
    public static AreaClinica paraEspecialidad(String especialidad) {
        if (especialidad == null) {
            return null;
        }
        String esp = especialidad.trim().toUpperCase();
        if ("PEDIATRIA".equals(esp)) {
            return PEDIATRIA;
        }
        if ("ODONTOLOGIA".equals(esp) || "ODONTOPEDIATRIA".equals(esp)) {
            return ODONTOLOGIA;
        }
        return null;
    }

    public static AreaClinica desdeSlug(String slug) {
        if (slug == null) {
            return null;
        }
        for (AreaClinica area : values()) {
            if (area.slug.equalsIgnoreCase(slug.trim())) {
                return area;
            }
        }
        return null;
    }
}
