package cl.duoc.barriodigital.mqadmin.domain;

/**
 * Datos internos para declarar una cola por AMQP.
 * El cuerpo HTTP ya quedó resuelto: durable, TTL y dead-letter llegan con un valor concreto.
 */
public record QueueDeclaration(
        String name,
        boolean durable,
        Integer ttlMillis,
        String deadLetterExchange,
        String deadLetterRoutingKey
) {
}
