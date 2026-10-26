package cl.duoc.barriodigital.mqadmin.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida una routing key.
 * Si no viene ninguna, la acepta, porque en fanout puede ir vacía.
 * Si viene, tiene que usar solo los caracteres permitidos.
 */
public class RoutingKeyValidator implements ConstraintValidator<RoutingKey, String> {

    /** Devuelve true si no hay texto o si el texto cumple el patrón. */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || MqNames.isRoutingKey(value);
    }
}
