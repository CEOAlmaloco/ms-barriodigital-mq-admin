package cl.duoc.barriodigital.mqadmin.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida el tipo de exchange.
 * Solo acepta direct, topic y fanout. Cualquier otro texto responde 400.
 */
public class ExchangeKindValidator implements ConstraintValidator<ExchangeKind, String> {

    /** Devuelve true solo para esas tres palabras, en minúsculas. */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return "direct".equals(value) || "topic".equals(value) || "fanout".equals(value);
    }
}
