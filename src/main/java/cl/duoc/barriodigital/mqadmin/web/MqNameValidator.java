package cl.duoc.barriodigital.mqadmin.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MqNameValidator implements ConstraintValidator<MqName, String> {

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
