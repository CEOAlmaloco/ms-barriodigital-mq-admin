package cl.duoc.barriodigital.mqadmin.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricción de una routing key.
 * Puede ir vacía. Si tiene texto, solo admite letras, números y unos pocos signos, hasta 100 caracteres.
 */
@Documented
@Constraint(validatedBy = RoutingKeyValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface RoutingKey {

    /** Frase que ve el cliente si la routing key no cumple el formato. */
    String message() default MqNames.ROUTING;

    /** Lo exige Jakarta Validation. mq-admin no agrupa restricciones. */
    Class<?>[] groups() default {};

    /** Lo exige Jakarta Validation. mq-admin no adjunta payload. */
    Class<? extends Payload>[] payload() default {};
}
