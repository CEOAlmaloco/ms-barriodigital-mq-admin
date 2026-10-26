package cl.duoc.barriodigital.mqadmin.domain;

/**
 * La cola, el exchange o el binding pedido no está en RabbitMQ.
 * La capa HTTP la convierte en 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    /** Conserva la frase, por ejemplo "La cola no existe". */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
