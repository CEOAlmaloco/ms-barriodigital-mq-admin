package cl.duoc.barriodigital.mqadmin.web;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MqErrorBody {

    private MqErrorBody() {
    }

    public static Map<String, Object> of(int status, String message, String path) {
        HttpStatus resolved = HttpStatus.resolve(status);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.UTC)));
        body.put("status", status);
        body.put("error", resolved == null ? "Error" : resolved.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return body;
    }
}
