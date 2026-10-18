package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.AlreadyExistsException;
import cl.duoc.barriodigital.mqadmin.domain.ConfigurationConflictException;
import cl.duoc.barriodigital.mqadmin.domain.InvalidArgumentException;
import cl.duoc.barriodigital.mqadmin.domain.ProtectedResourceException;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class MqExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        return ResponseEntity.status(status).body(body(status.value(), ex.getReason() == null ? "" : ex.getReason()));
    }

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(InvalidArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(400, ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(404, ex.getMessage()));
    }

    @ExceptionHandler({
            ProtectedResourceException.class,
            AlreadyExistsException.class,
            ConfigurationConflictException.class
    })
    public ResponseEntity<Map<String, Object>> conflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(409, ex.getMessage()));
    }

    private static Map<String, Object> body(int status, String message) {
        HttpStatus resolved = HttpStatus.resolve(status);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", resolved == null ? "Error" : resolved.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
