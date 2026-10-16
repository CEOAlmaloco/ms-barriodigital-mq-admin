package cl.duoc.barriodigital.mqadmin.domain;

public class ProtectedResourceException extends RuntimeException {

    public ProtectedResourceException() {
        super("El nombre está reservado para la topología");
    }
}
