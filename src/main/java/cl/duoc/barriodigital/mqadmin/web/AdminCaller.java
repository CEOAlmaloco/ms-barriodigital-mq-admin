package cl.duoc.barriodigital.mqadmin.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El BFF valida el JWT y reenvía el claim {@code roles} en este header.
 * mq-admin no lee el token. Los endpoints de exchanges y bindings usan la misma comprobación.
 */
public final class AdminCaller {

    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String FORBIDDEN_MESSAGE = "Solo Admin puede administrar las colas";

    private AdminCaller() {
    }

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

    public static void requireAdmin(String rolesHeader) {
        if (!isAdmin(rolesHeader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE);
        }
    }
}
