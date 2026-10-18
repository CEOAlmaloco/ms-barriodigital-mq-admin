package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.InvalidArgumentException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Set;

public final class MqPayloadValidator {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static final List<String> PRIORITY = List.of(
            MqNames.AMQ,
            MqNames.FORMAT,
            cl.duoc.barriodigital.mqadmin.domain.ExchangeType.INVALID_MESSAGE,
            MqNames.TTL,
            MqNames.DLX,
            MqNames.ROUTING);

    private MqPayloadValidator() {
    }

    public static void check(Object body) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(body);
        if (violations.isEmpty()) {
            return;
        }
        throw new InvalidArgumentException(first(violations));
    }

    public static void requireName(String name) {
        String message = MqNames.nameMessage(name);
        if (message != null) {
            throw new InvalidArgumentException(message);
        }
    }

    public static void requireRoutingKey(String routingKey) {
        if (!MqNames.isRoutingKey(routingKey)) {
            throw new InvalidArgumentException(MqNames.ROUTING);
        }
    }

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
