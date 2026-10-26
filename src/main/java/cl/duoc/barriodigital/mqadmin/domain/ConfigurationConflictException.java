package cl.duoc.barriodigital.mqadmin.domain;

/**
 * El nombre ya existe en RabbitMQ con otras propiedades.
 * Ocurre, por ejemplo, al declarar la misma cola con otro TTL. La capa HTTP
 * responde 409.
 */
public class ConfigurationConflictException extends RuntimeException {

    /** Arma el mensaje con la palabra cola, exchange o binding. */
    public ConfigurationConflictException(String resource) {
        super("La configuración de " + resource + " no coincide con la que ya existe");
    }
}
