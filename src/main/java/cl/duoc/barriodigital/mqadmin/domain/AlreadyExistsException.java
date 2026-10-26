package cl.duoc.barriodigital.mqadmin.domain;

/**
 * El recurso pedido ya estaba creado con la misma configuración.
 * La capa HTTP la convierte en 409.
 */
public class AlreadyExistsException extends RuntimeException {

    /** Conserva el mensaje que verá el cliente, por ejemplo "La cola ya existe". */
    public AlreadyExistsException(String message) {
        super(message);
    }
}
