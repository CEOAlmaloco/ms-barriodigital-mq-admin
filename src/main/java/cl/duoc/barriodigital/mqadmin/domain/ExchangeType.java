package cl.duoc.barriodigital.mqadmin.domain;

import java.util.Locale;

public enum ExchangeType {
    DIRECT,
    TOPIC,
    FANOUT;

    public static final String INVALID_MESSAGE = "El tipo de exchange debe ser direct, topic o fanout";

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

    public String wireName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
