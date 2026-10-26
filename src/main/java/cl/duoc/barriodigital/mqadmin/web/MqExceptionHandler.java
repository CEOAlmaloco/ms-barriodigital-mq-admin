package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.AlreadyExistsException;
import cl.duoc.barriodigital.mqadmin.domain.ConfigurationConflictException;
import cl.duoc.barriodigital.mqadmin.domain.InvalidArgumentException;
import cl.duoc.barriodigital.mqadmin.domain.ProtectedResourceException;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Traduce las excepciones de mq-admin a un JSON que el cliente puede leer.
 * Un dato mal escrito es 400, la falta de Admin es 403, un recurso ausente es 404 y un nombre reservado o repetido es 409.
 */
@RestControllerAdvice
public class MqExceptionHandler {

    /** Un 403 de Admin, u otra ResponseStatusException, se responde con su propio código. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex, HttpServletRequest request) {
        int code = ex.getStatusCode().value();
        return ResponseEntity.status(code).body(MqErrorBody.of(code, ex.getReason() == null ? "" : ex.getReason(), request.getRequestURI()));
    }

    /** Un dato mal escrito, incluido un nombre amq., responde 400. */
    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(InvalidArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(MqErrorBody.of(400, ex.getMessage(), request.getRequestURI()));
    }

    /** Un JSON que no se puede leer responde 400, con una frase según el campo roto. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MqErrorBody.of(400, readableMessage(ex), request.getRequestURI()));
    }

    /** Una cola, un exchange o un binding ausente responde 404. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(MqErrorBody.of(404, ex.getMessage(), request.getRequestURI()));
    }

    /** Un nombre cmd.* o q.cmd.* reservado, un recurso repetido o propiedades distintas responden 409. */
    @ExceptionHandler({
            ProtectedResourceException.class,
            AlreadyExistsException.class,
            ConfigurationConflictException.class
    })
    public ResponseEntity<Map<String, Object>> conflict(RuntimeException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(MqErrorBody.of(409, ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Elige la frase cuando el JSON no se pudo leer.
     * Si el campo roto es ttl o durable, usa su mensaje. En cualquier otro caso dice que el cuerpo no cumple el formato.
     */
    private static String readableMessage(HttpMessageNotReadableException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof InvalidFormatException invalid && !invalid.getPath().isEmpty()) {
                String field = invalid.getPath().get(invalid.getPath().size() - 1).getFieldName();
                if ("ttl".equals(field)) {
                    return MqNames.TTL;
                }
                if ("durable".equals(field)) {
                    return MqNames.DURABLE;
                }
            }
            current = current.getCause();
        }
        return MqNames.BODY;
    }
}
