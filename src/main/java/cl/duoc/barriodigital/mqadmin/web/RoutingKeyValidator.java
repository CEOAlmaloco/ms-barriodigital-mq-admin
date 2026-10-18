package cl.duoc.barriodigital.mqadmin.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RoutingKeyValidator implements ConstraintValidator<RoutingKey, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || MqNames.isRoutingKey(value);
    }
}
