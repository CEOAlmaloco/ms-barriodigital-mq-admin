package cl.duoc.barriodigital.mqadmin.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricción de un nombre de cola, exchange o binding.
 * Exige de 1 a 100 caracteres del alfabeto permitido y rechaza el prefijo amq. con 400.
 */
@Documented
@Constraint(validatedBy = MqNameValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MqName {

    /** Frase por defecto. El validador la cambia si el problema es el prefijo amq. */
    String message() default MqNames.FORMAT;

    /** Lo exige Jakarta Validation. mq-admin no agrupa restricciones. */
    Class<?>[] groups() default {};

    /** Lo exige Jakarta Validation. mq-admin no adjunta payload. */
    Class<? extends Payload>[] payload() default {};
}
