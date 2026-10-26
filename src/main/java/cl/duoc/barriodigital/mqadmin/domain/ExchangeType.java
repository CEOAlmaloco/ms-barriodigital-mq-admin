package cl.duoc.barriodigital.mqadmin.domain;

import java.util.Locale;

/**
 * Tipos de exchange que acepta mq-admin.
 * direct exige routing key exacta, topic admite patrones y fanout entrega a cada cola enlazada sin usar la routing key.
 */
public enum ExchangeType {
    DIRECT,
    TOPIC,
    FANOUT;

    public static final String INVALID_MESSAGE = "El tipo de exchange debe ser direct, topic o fanout";

    /** Convierte el texto del JSON. Solo acepta direct, topic o fanout en minúsculas. */
    public static ExchangeType parse(String raw) {
        if ("direct".equals(raw)) {
            return DIRECT;
        }
        if ("topic".equals(raw)) {
            return TOPIC;
        }
        if ("fanout".equals(raw)) {
            return FANOUT;
        }
        throw new InvalidArgumentException(INVALID_MESSAGE);
    }

    /** Devuelve el nombre en minúsculas, que es el valor que escribe RabbitMQ. */
    public String wireName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
