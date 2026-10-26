package cl.duoc.barriodigital.mqadmin.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida un nombre de cola, exchange o binding.
 * Si el formato no sirve, informa ese fallo. Si empieza por amq., informa que ese prefijo responde 400.
 */
public class MqNameValidator implements ConstraintValidator<MqName, String> {

    /** Devuelve true si el nombre es válido. Si no, escribe la frase exacta en el error. */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String message = MqNames.nameMessage(value);
        if (message == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
