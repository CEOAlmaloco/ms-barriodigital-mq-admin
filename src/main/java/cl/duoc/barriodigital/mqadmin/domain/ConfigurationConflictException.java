package cl.duoc.barriodigital.mqadmin.domain;

public class ConfigurationConflictException extends RuntimeException {

    public ConfigurationConflictException(String resource) {
        super("La configuración de " + resource + " no coincide con la que ya existe");
    }
}
