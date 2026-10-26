package cl.duoc.barriodigital.mqadmin.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Comprueba si quien llama es Admin.
 * El BFF valida el JWT y reenvía el claim roles en X-User-Roles. mq-admin no lee el token.
 * Exchanges y bindings usan la misma comprobación que las colas.
 */
public final class AdminCaller {

    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String FORBIDDEN_MESSAGE = "Solo Admin puede administrar las colas";

    /** Nadie instancia esta clase: solo se usan los métodos estáticos. */
    private AdminCaller() {
    }

    /**
     * Devuelve true si en la lista de roles aparece Admin o ROLE_Admin.
     * La lista viene separada por comas y no distingue mayúsculas.
     * Un header vacío o null no es Admin.
     */
    public static boolean isAdmin(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }
        String[] parts = rolesHeader.split(",");
        for (String part : parts) {
            String role = part.trim();
            if (role.equalsIgnoreCase("Admin") || role.equalsIgnoreCase("ROLE_Admin")) {
                return true;
            }
        }
        return false;
    }

    /** Si no es Admin, corta con 403 y el mensaje "Solo Admin puede administrar las colas". */
    public static void requireAdmin(String rolesHeader) {
        if (!isAdmin(rolesHeader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE);
        }
    }
}
