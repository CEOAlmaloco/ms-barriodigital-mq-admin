package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.QueueDeclaration;

/**
 * Respuesta al listar, consultar o crear una cola.
 * messages es el total del Management: listos más sin confirmar.
 * Incluye durable, el TTL y el destino de dead-letter.
 */
public record QueueView(
        String name,
        int messages,
        int consumers,
        boolean durable,
        Integer ttl,
        String deadLetterExchange,
        String deadLetterRoutingKey
) {

    /** Arma la respuesta de una cola recién creada: 0 mensajes y 0 consumidores. */
    public static QueueView created(QueueDeclaration declaration) {
        return new QueueView(
                declaration.name(),
                0,
                0,
                declaration.durable(),
                declaration.ttlMillis(),
                declaration.deadLetterExchange(),
                declaration.deadLetterRoutingKey());
    }
}
