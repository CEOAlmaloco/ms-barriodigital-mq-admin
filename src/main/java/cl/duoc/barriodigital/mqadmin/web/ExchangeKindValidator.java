package cl.duoc.barriodigital.mqadmin.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ExchangeKindValidator implements ConstraintValidator<ExchangeKind, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return "direct".equals(value) || "topic".equals(value) || "fanout".equals(value);
    }
}
