package cl.duoc.barriodigital.mqadmin.web;

import java.util.regex.Pattern;

/**
 * Reglas de nombres, routing keys y frases que ve el cliente.
 * Un nombre que empieza por amq. es inválido y responde 400. Los reservados cmd.* y q.cmd.* responden 409 en otra capa.
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

    /** Nadie instancia esta clase: solo guarda constantes y métodos estáticos. */
    private MqNames() {
    }

    /**
     * Revisa un nombre.
     * Devuelve null si está bien. Si no, devuelve la frase de formato o la frase de amq., que es un 400.
     */
    public static String nameMessage(String value) {
        if (value == null || !NAME.matcher(value).matches()) {
            return FORMAT;
        }
        if (value.startsWith("amq.")) {
            return AMQ;
        }
        return null;
    }

    /** Indica si la routing key tiene solo los caracteres permitidos y no pasa de 100. */
    public static boolean isRoutingKey(String value) {
        return value != null && ROUTING_KEY.matcher(value).matches();
    }
}
