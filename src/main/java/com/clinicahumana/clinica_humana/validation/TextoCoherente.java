package com.clinicahumana.clinica_humana.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Restricción Bean Validation que rechaza textos clínicos incoherentes:
 * cadenas aleatorias de teclado, letras repetidas y descripciones demasiado cortas.
 * El valor vacío se acepta aquí (la obligatoriedad la controla @NotBlank).
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = TextoCoherenteValidator.class)
public @interface TextoCoherente {

    String message() default "La descripción no es válida: debe escribir una explicación clínica real con palabras coherentes separadas por espacios (mínimo 10 caracteres), sin cadenas aleatorias ni letras repetidas.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
