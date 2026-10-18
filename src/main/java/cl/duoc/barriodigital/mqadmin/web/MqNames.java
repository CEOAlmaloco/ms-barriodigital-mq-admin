package cl.duoc.barriodigital.mqadmin.web;

import java.util.regex.Pattern;

/**
 * Reglas del contrato de mq-admin. {@code amq.} es nombre inválido (400), no un recurso reservado.
 */
public final class MqNames {

    public static final String FORMAT = "El nombre no cumple el formato";
    public static final String AMQ = "El nombre no puede empezar por amq.";
    public static final String TTL = "El TTL debe ser un entero entre 1 y 86400000";
    public static final String DLX = "La routing key de dead-letter exige un exchange";
    public static final String ROUTING = "La routing key no cumple el formato";
    public static final String DURABLE = "durable debe ser true o false";
    public static final String BODY = "El cuerpo no cumple el formato";

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");
    private static final Pattern ROUTING_KEY = Pattern.compile("^[A-Za-z0-9._#*-]{0,100}$");

    private MqNames() {
    }

    public static String nameMessage(String value) {
        if (value == null || !NAME.matcher(value).matches()) {
            return FORMAT;
        }
        if (value.startsWith("amq.")) {
            return AMQ;
        }
        return null;
    }

    public static boolean isRoutingKey(String value) {
        return value != null && ROUTING_KEY.matcher(value).matches();
    }
}
