package cl.duoc.barriodigital.mqadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Nombres exactos y prefijos que no se pueden crear, borrar ni vaciar.
 * Los reservados {@code cmd.*} y {@code q.cmd.*} responden 409. Un nombre {@code amq.} responde 400 y no se trata aquí.
 */
@Component
@ConfigurationProperties(prefix = "barriodigital.mq")
public class MqProtectionProperties {

    private List<String> protectedNames = new ArrayList<>();
    private List<String> protectedPrefixes = new ArrayList<>();

    /**
     * Indica si el nombre está prohibido.
     * Un nombre vacío también cuenta como prohibido, para no operar sin identificador.
     * Compara primero la lista exacta y después los prefijos.
     */
    public boolean isProtected(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        for (String exact : protectedNames) {
            if (name.equals(exact)) {
                return true;
            }
        }
        for (String prefix : protectedPrefixes) {
            if (prefix != null && !prefix.isEmpty() && name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Devuelve los nombres exactos que no se pueden modificar. */
    public List<String> getProtectedNames() {
        return protectedNames;
    }

    /** Spring asigna la lista al arrancar. Si el yml no trae nombres, deja una lista vacía. */
    public void setProtectedNames(List<String> protectedNames) {
        this.protectedNames = protectedNames == null ? new ArrayList<>() : protectedNames;
    }

    /** Devuelve los prefijos prohibidos, por ejemplo {@code cmd.} y {@code q.cmd.}. */
    public List<String> getProtectedPrefixes() {
        return protectedPrefixes;
    }

    /** Spring asigna los prefijos al arrancar. Si el yml no trae prefijos, deja una lista vacía. */
    public void setProtectedPrefixes(List<String> protectedPrefixes) {
        this.protectedPrefixes = protectedPrefixes == null ? new ArrayList<>() : protectedPrefixes;
    }
}
