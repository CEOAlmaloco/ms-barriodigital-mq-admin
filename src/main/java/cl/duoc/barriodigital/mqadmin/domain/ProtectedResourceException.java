package cl.duoc.barriodigital.mqadmin.domain;

/**
 * El nombre está reservado para la topología y no se puede crear, borrar ni vaciar.
 * Los prefijos {@code cmd.*} y {@code q.cmd.*} responden 409. Un nombre {@code amq.} responde 400 antes de llegar aquí.
 */
public class ProtectedResourceException extends RuntimeException {

    /** Usa siempre el mismo mensaje: el nombre está reservado para la topología. */
    public ProtectedResourceException() {
        super("El nombre está reservado para la topología");
    }
}
