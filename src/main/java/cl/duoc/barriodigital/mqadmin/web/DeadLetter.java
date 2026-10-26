package cl.duoc.barriodigital.mqadmin.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricción de dead-letter sobre el cuerpo de una cola.
 * Si se indica la routing key de los mensajes no entregados, el exchange de dead-letter también es obligatorio.
 */
@Documented
@Constraint(validatedBy = DeadLetterValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DeadLetter {

    /** Mensaje que se usa si el validador no pone uno más preciso. */
    String message() default MqNames.DLX;

    /** Lo exige Jakarta Validation. mq-admin no agrupa restricciones. */
    Class<?>[] groups() default {};

    /** Lo exige Jakarta Validation. mq-admin no adjunta payload. */
    Class<? extends Payload>[] payload() default {};
}
