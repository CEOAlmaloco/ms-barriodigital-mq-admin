package cl.duoc.barriodigital.mqadmin.web;

public record QueueRequest(
        String name,
        Boolean durable,
        Integer ttl,
        String deadLetterExchange,
        String deadLetterRoutingKey
) {
}
