package cl.duoc.barriodigital.mqadmin.adapter.in.web.dto;

import cl.duoc.barriodigital.mqadmin.domain.QueueDeclaration;

/**
 * Respuesta al listar, consultar o crear una cola.
 * {@code messages} es el total del Management (listos + sin confirmar).
 * Ready/Unacked, estado, memoria y tasas salen del mismo inventario HTTP.
 */
public record QueueView(
        String name,
        int messages,
        int messagesReady,
        int messagesUnacked,
        int consumers,
        boolean durable,
        Integer ttl,
        String deadLetterExchange,
        String deadLetterRoutingKey,
        String state,
        Long memoryBytes,
        Double publishRate,
        Double deliverRate,
        Double ackRate
) {

    /** Arma la respuesta de una cola recién creada: 0 mensajes y 0 consumidores. */
    public static QueueView created(QueueDeclaration declaration) {
        return new QueueView(
                declaration.name(),
                0,
                0,
                0,
                0,
                declaration.durable(),
                declaration.ttlMillis(),
                declaration.deadLetterExchange(),
                declaration.deadLetterRoutingKey(),
                null,
                null,
                null,
                null,
                null);
    }
}
