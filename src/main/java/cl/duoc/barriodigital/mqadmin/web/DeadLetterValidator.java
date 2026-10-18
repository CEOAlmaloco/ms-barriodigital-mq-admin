package cl.duoc.barriodigital.mqadmin.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DeadLetterValidator implements ConstraintValidator<DeadLetter, QueueRequest> {

    @Override
    public boolean isValid(QueueRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String exchange = value.deadLetterExchange();
        String routingKey = value.deadLetterRoutingKey();
        boolean exchangeBlank = exchange == null || exchange.isBlank();
        boolean keyPresent = routingKey != null && !routingKey.isBlank();
        if (keyPresent && exchangeBlank) {
            return reject(context, MqNames.DLX);
        }
        if (!exchangeBlank) {
            String nameMessage = MqNames.nameMessage(exchange);
            if (nameMessage != null) {
                return reject(context, nameMessage);
            }
        }
        if (routingKey != null && !routingKey.isEmpty() && !MqNames.isRoutingKey(routingKey)) {
            return reject(context, MqNames.ROUTING);
        }
        return true;
    }

    private static boolean reject(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
