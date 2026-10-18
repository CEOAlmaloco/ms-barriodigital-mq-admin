package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.QueueDeclaration;

/**
 * {@code messages} es el total del Management: listos más sin confirmar.
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
