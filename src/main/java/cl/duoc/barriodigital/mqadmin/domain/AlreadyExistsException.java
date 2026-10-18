package cl.duoc.barriodigital.mqadmin.domain;

public class AlreadyExistsException extends RuntimeException {

    public AlreadyExistsException(String message) {
        super(message);
    }
}
