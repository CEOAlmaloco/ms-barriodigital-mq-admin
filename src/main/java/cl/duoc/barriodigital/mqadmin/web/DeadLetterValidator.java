package cl.duoc.barriodigital.mqadmin.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida el dead-letter de una cola.
 * Si se indica la routing key y falta el exchange, no deja crear la cola.
 * También comprueba que el nombre y la routing key cumplan el formato. Un exchange amq. responde 400.
 */
public class DeadLetterValidator implements ConstraintValidator<DeadLetter, QueueRequest> {

    /**
     * Devuelve true si el dead-letter es válido.
     * Un cuerpo null se acepta: otro validador cubre el cuerpo vacío.
     * Si el exchange viene, tiene que ser un nombre legal. Si la routing key viene, tiene que cumplir el formato.
     */
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

    /** Sustituye el mensaje genérico por la frase concreta y marca el pedido como inválido. */
    private static boolean reject(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
