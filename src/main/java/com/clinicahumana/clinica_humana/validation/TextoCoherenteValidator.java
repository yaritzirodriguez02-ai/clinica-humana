package com.clinicahumana.clinica_humana.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Ejecuta {@link ValidacionesClinicas#textoClinicoValido(String)} sobre los campos anotados.
 * Los campos opcionales (vacíos) pasan la validación: su obligatoriedad se controla con @NotBlank.
 */
public class TextoCoherenteValidator implements ConstraintValidator<TextoCoherente, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return ValidacionesClinicas.textoClinicoValido(value);
    }
}
