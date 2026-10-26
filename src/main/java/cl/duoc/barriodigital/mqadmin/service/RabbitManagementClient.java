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
 * Consulta RabbitMQ por la API HTTP de Management.
 * Lista colas, lee una cola, lee un exchange y comprueba si un binding ya existe.
 * AMQP no publica ese inventario, por eso el listado no sale de AmqpAdmin.
 */
@Component
public class RabbitManagementClient {

    private final RestClient http;
    private final URI base;
    private final String vhost;

    /**
     * Arma el cliente HTTP.
     * Si el vhost viene vacío, usa /. Quita las barras finales de la URL y guarda usuario y clave.
     */
    public RabbitManagementClient(MqManagementProperties properties) {
        this.vhost = properties.getVhost() == null || properties.getVhost().isBlank() ? "/" : properties.getVhost();
        String url = properties.getUrl() == null ? "" : properties.getUrl().replaceAll("/+$", "");
        this.base = URI.create(url);
        this.http = RestClient.builder()
                .baseUrl(url)
                .defaultHeaders(headers -> headers.setBasicAuth(properties.getUsername(), properties.getPassword()))
                .build();
    }

    /** Pide todas las colas del vhost y las convierte en QueueView. */
    public List<QueueView> listQueues() {
        JsonNode body = get("/api/queues/" + encode(vhost));
        List<QueueView> queues = new ArrayList<>();
        if (body != null && body.isArray()) {
            body.forEach(node -> queues.add(toView(node)));
        }
        return queues;
    }

    /** Pide una cola. Si el Management responde 404, lanza "La cola no existe". */
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

    /** Pide un exchange. Si el Management responde 404, lanza "El exchange no existe". */
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

    /**
     * Indica si ya existe un binding con esa routing key entre ese exchange y esa cola.
     * Si el Management responde 404, devuelve false y no lanza error.
     */
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

    /** Ejecuta el GET y devuelve el JSON. */
    private JsonNode get(String path) {
        return http.get().uri(base.resolve(path)).retrieve().body(JsonNode.class);
    }

    /** Copia nombre, tipo y durable desde el JSON del Management. */
    static ExchangeView toExchange(JsonNode node) {
        return new ExchangeView(
                node.path("name").asText(),
                node.path("type").asText(),
                node.path("durable").asBoolean(true));
    }

    /** Copia nombre, mensajes, consumidores, durable, TTL y dead-letter desde el JSON del Management. */
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

    /** Lee un número de los argumentos. Si el campo no viene, devuelve null. */
    private static Integer numberOrNull(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asInt();
    }

    /** Lee un texto de los argumentos. Si no viene o está en blanco, devuelve null. */
    private static String textOrNull(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    /** Codifica el segmento para que una barra, como la del vhost /, no rompa la URL. */
    private static String encode(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }
}
