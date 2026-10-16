package cl.duoc.barriodigital.mqadmin.domain;

public record QueueDeclaration(
        String name,
        boolean durable,
        Integer ttlMillis,
        String deadLetterExchange,
        String deadLetterRoutingKey
) {
}
