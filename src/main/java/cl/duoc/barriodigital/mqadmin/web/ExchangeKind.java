package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.ExchangeType;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricción del campo type de un exchange.
 * Solo admite direct, topic o fanout.
 */
@Documented
@Constraint(validatedBy = ExchangeKindValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExchangeKind {

    /** Frase que ve el cliente si el tipo no es uno de los tres. */
    String message() default ExchangeType.INVALID_MESSAGE;

    /** Lo exige Jakarta Validation. mq-admin no agrupa restricciones. */
    Class<?>[] groups() default {};

    /** Lo exige Jakarta Validation. mq-admin no adjunta payload. */
    Class<? extends Payload>[] payload() default {};
}
