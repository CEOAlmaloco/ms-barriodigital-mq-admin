package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.InvalidArgumentException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Set;

/**
 * Revisa el pedido antes de tocar RabbitMQ.
 * Si hay varios fallos a la vez, elige una sola frase para que el 400 sea unívoco.
 */
public final class MqPayloadValidator {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static final List<String> PRIORITY = List.of(
            MqNames.AMQ,
            MqNames.FORMAT,
            cl.duoc.barriodigital.mqadmin.domain.ExchangeType.INVALID_MESSAGE,
            MqNames.TTL,
            MqNames.DLX,
            MqNames.ROUTING);

    /** Nadie instancia esta clase: solo se usan los métodos estáticos. */
    private MqPayloadValidator() {
    }

    /** Ejecuta las restricciones del cuerpo. Si alguna falla, lanza dato inválido con la frase de mayor prioridad. */
    public static void check(Object body) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(body);
        if (violations.isEmpty()) {
            return;
        }
        throw new InvalidArgumentException(first(violations));
    }

    /** Revisa un nombre que vino en la URL, fuera del JSON. Un prefijo amq. responde 400. */
    public static void requireName(String name) {
        String message = MqNames.nameMessage(name);
        if (message != null) {
            throw new InvalidArgumentException(message);
        }
    }

    /** Revisa una routing key que vino como parámetro de la URL. */
    public static void requireRoutingKey(String routingKey) {
        if (!MqNames.isRoutingKey(routingKey)) {
            throw new InvalidArgumentException(MqNames.ROUTING);
        }
    }

    /**
     * Elige un solo mensaje cuando hay varios.
     * El orden es prefijo amq., formato, tipo de exchange, TTL, dead-letter y al final la routing key.
     */
    private static String first(Set<ConstraintViolation<Object>> violations) {
        List<String> messages = violations.stream().map(ConstraintViolation::getMessage).toList();
        for (String preferred : PRIORITY) {
            if (messages.contains(preferred)) {
                return preferred;
            }
        }
        return messages.getFirst();
    }
}
