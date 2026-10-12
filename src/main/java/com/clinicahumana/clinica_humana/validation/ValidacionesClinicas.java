package com.clinicahumana.clinica_humana.validation;

import java.util.regex.Pattern;

/**
 * Utilidad compartida de validación clínica.
 * Estas reglas se aplican en los controladores/entidades de Java y están espejadas
 * en el JavaScript de las plantillas Thymeleaf para bloquear texto basura
 * (cadenas aleatorias de teclado, letras repetidas, descripciones sin sentido).
 */
public final class ValidacionesClinicas {

    /** Valor canónico que se almacena en BD cuando el médico firma en formato físico. */
    public static final String FIRMA_FISICO = "FIRMA EN FÍSICO";

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    // ------------------------------------------------------------------
    // Repeticiones sin sentido: "aaaa", "abcabcabcabc", "tatatata", etc.
    // ------------------------------------------------------------------
    private static final Pattern REPETICIONES = Pattern.compile("([\\p{L}]{2,5})\\1{3,}");

    // Palabras de 4+ letras sin vocales -> típico mashing de teclado ("asdfgh")
    private static final Pattern VOCALES = Pattern.compile("[aeiouáéíóúü]", FLAGS);

    // ------------------------------------------------------------------
    // Recetas: formatos estructurados y obligatorios
    // ------------------------------------------------------------------

    /** Dosis: número (o fracción) seguido de unidad (ej. "5 ml", "10 mg", "2 gotas", "1/2 cda"). */
    public static final String DOSIS_REGEX =
            "^(?:\\d{1,3}(?:[.,]\\d{1,2})?|\\d{1,3}\\s*/\\s*\\d{1,2})\\s*" +
            "(?:ml/kg|mg/kg|mcg/kg|gotas?|gts?|comprimidos?|capsulas?|cucharaditas?|cucharadas?|sobres?|parches?" +
            "|unidad(?:es)?|ml|mg|mcg|g|cg|ui|comp\\.?|caps\\.?|cda\\.?)$";

    /** Frecuencia ("Cada"): número + unidad de tiempo (ej. "cada 8 horas", "8 hrs", "c/8h", "q8h"). */
    public static final String FRECUENCIA_REGEX =
            "^(?:cada\\s+|c/\\s*|q)?\\d{1,3}(?:\\s*-\\s*\\d{1,3})?\\s*" +
            "(?:horas?|hrs?|h|d[ií]as?|d|semanas?|minutos?|min)$";

    /** Duración ("Por"): número + unidad de tiempo (ej. "por 5 días", "5 dias", "7-10 días"). */
    public static final String DURACION_REGEX =
            "^(?:por\\s+)?\\d{1,3}(?:\\s*-\\s*\\d{1,3})?\\s*" +
            "(?:d[ií]as?|d|semanas?|meses)$";

    /** Nombre de medicamento: letras/números y signos farmacéuticos básicos, sin texto aleatorio. */
    public static final String MEDICAMENTO_REGEX =
            "^[\\p{L}\\d][\\p{L}\\d\\s\\-\\.\\(\\)%+,/']{2,119}$";

    public static final Pattern DOSIS = Pattern.compile(DOSIS_REGEX, FLAGS);
    public static final Pattern FRECUENCIA = Pattern.compile(FRECUENCIA_REGEX, FLAGS);
    public static final Pattern DURACION = Pattern.compile(DURACION_REGEX, FLAGS);
    public static final Pattern MEDICAMENTO = Pattern.compile(MEDICAMENTO_REGEX);

    private ValidacionesClinicas() {
    }

    // ------------------------------------------------------------------
    // Descripciones clínicas (diagnóstico, tratamiento, evaluación, etc.)
    // ------------------------------------------------------------------

    /** Regla general: mínimo 10 caracteres y 2 palabras coherentes. */
    public static boolean textoClinicoValido(String texto) {
        return pasaFiltroBasura(texto, 2, 10);
    }

    /** Recomendaciones e indicaciones generales: mínimo 15 caracteres y 3 palabras coherentes. */
    public static boolean recomendacionValida(String texto) {
        return pasaFiltroBasura(texto, 3, 15);
    }

    private static boolean pasaFiltroBasura(String texto, int minPalabras, int minCaracteres) {
        if (texto == null) {
            return false;
        }
        String v = texto.trim();
        if (v.length() < minCaracteres) {
            return false;
        }
        String[] palabras = v.split("\\s+");
        if (palabras.length < minPalabras) {
            return false;
        }
        if (REPETICIONES.matcher(v).find()) {
            return false;
        }
        int letras = 0;
        for (String p : palabras) {
            if (p.length() > 22) {
                return false;
            }
            String soloLetras = p.replaceAll("[^\\p{L}]", "");
            letras += soloLetras.length();
            if (soloLetras.length() >= 4 && !VOCALES.matcher(soloLetras).find()) {
                return false;
            }
        }
        return letras >= 8;
    }

    // ------------------------------------------------------------------
    // Recetas
    // ------------------------------------------------------------------

    public static boolean medicamentoValido(String medicamento) {
        if (medicamento == null) {
            return false;
        }
        String m = medicamento.trim();
        if (!MEDICAMENTO.matcher(m).matches()) {
            return false;
        }
        if (REPETICIONES.matcher(m).find()) {
            return false;
        }
        int letras = 0;
        for (String p : m.split("\\s+")) {
            if (p.length() > 30) {
                return false;
            }
            String soloLetras = p.replaceAll("[^\\p{L}]", "");
            letras += soloLetras.length();
            if (soloLetras.length() >= 4 && !VOCALES.matcher(soloLetras).find()) {
                return false;
            }
        }
        return letras >= 4;
    }

    public static boolean dosisValida(String dosis) {
        return dosis != null && DOSIS.matcher(dosis.trim()).matches();
    }

    public static boolean frecuenciaValida(String frecuencia) {
        return frecuencia != null && FRECUENCIA.matcher(frecuencia.trim()).matches();
    }

    public static boolean duracionValida(String duracion) {
        return duracion != null && DURACION.matcher(duracion.trim()).matches();
    }
}
