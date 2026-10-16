package cl.duoc.barriodigital.mqadmin.domain;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
