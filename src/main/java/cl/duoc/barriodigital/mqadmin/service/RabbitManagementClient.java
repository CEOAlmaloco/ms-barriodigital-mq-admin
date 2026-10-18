package cl.duoc.barriodigital.mqadmin.service;

import cl.duoc.barriodigital.mqadmin.config.MqManagementProperties;
import cl.duoc.barriodigital.mqadmin.domain.BindingDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.web.ExchangeView;
import cl.duoc.barriodigital.mqadmin.web.QueueView;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * El listado no sale de {@code AmqpAdmin}: RabbitMQ solo lo publica en la HTTP API del Management.
 */
@Component
public class RabbitManagementClient {

    private final RestClient http;
    private final URI base;
    private final String vhost;

    public RabbitManagementClient(MqManagementProperties properties) {
        this.vhost = properties.getVhost() == null || properties.getVhost().isBlank() ? "/" : properties.getVhost();
        String url = properties.getUrl() == null ? "" : properties.getUrl().replaceAll("/+$", "");
        this.base = URI.create(url);
        this.http = RestClient.builder()
                .baseUrl(url)
                .defaultHeaders(headers -> headers.setBasicAuth(properties.getUsername(), properties.getPassword()))
                .build();
    }

    public List<QueueView> listQueues() {
        JsonNode body = get("/api/queues/" + encode(vhost));
        List<QueueView> queues = new ArrayList<>();
        if (body != null && body.isArray()) {
            body.forEach(node -> queues.add(toView(node)));
        }
        return queues;
    }

    public QueueView getQueue(String name) {
        try {
            JsonNode body = get("/api/queues/" + encode(vhost) + "/" + encode(name));
            if (body == null || body.isMissingNode()) {
                throw new ResourceNotFoundException("La cola no existe");
            }
            return toView(body);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("La cola no existe");
            }
            throw ex;
        }
    }

    public ExchangeView getExchange(String name) {
        try {
            JsonNode body = get("/api/exchanges/" + encode(vhost) + "/" + encode(name));
            if (body == null || body.isMissingNode()) {
                throw new ResourceNotFoundException("El exchange no existe");
            }
            return toExchange(body);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("El exchange no existe");
            }
            throw ex;
        }
    }

    public boolean hasBinding(BindingDeclaration declaration) {
        String routingKey = declaration.routingKey() == null ? "" : declaration.routingKey();
        String path = "/api/bindings/" + encode(vhost)
                + "/e/" + encode(declaration.exchange())
                + "/q/" + encode(declaration.queue());
        try {
            JsonNode body = get(path);
            if (body == null || !body.isArray()) {
                return false;
            }
            for (JsonNode node : body) {
                if (routingKey.equals(node.path("routing_key").asText(""))) {
                    return true;
                }
            }
            return false;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return false;
            }
            throw ex;
        }
    }

    private JsonNode get(String path) {
        return http.get().uri(base.resolve(path)).retrieve().body(JsonNode.class);
    }

    static ExchangeView toExchange(JsonNode node) {
        return new ExchangeView(
                node.path("name").asText(),
                node.path("type").asText(),
                node.path("durable").asBoolean(true));
    }

    static QueueView toView(JsonNode node) {
        JsonNode arguments = node.path("arguments");
        return new QueueView(
                node.path("name").asText(),
                node.path("messages").asInt(0),
                node.path("consumers").asInt(0),
                node.path("durable").asBoolean(true),
                numberOrNull(arguments, "x-message-ttl"),
                textOrNull(arguments, "x-dead-letter-exchange"),
                textOrNull(arguments, "x-dead-letter-routing-key"));
    }

    private static Integer numberOrNull(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asInt();
    }

    private static String textOrNull(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private static String encode(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }
}
