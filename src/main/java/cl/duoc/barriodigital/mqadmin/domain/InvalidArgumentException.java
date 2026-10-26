package cl.duoc.barriodigital.mqadmin.domain;

/**
 * El cuerpo o un parámetro no cumple el contrato.
 * Cubre un nombre inválido, un tipo desconocido o un TTL fuera de rango. La capa HTTP responde 400.
 */
public class InvalidArgumentException extends RuntimeException {

    /** Conserva la frase que explica qué dato está mal. */
    public InvalidArgumentException(String message) {
        super(message);
    }
}
