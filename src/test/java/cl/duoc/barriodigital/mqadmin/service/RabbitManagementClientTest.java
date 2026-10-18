package cl.duoc.barriodigital.mqadmin.service;

import cl.duoc.barriodigital.mqadmin.config.MqManagementProperties;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.web.QueueView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RabbitManagementClientTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void elTotalDeMensajesSaleDelCampoMessages() throws Exception {
        String json = """
                {"name":"q.cmd.email","messages":3,"messages_ready":2,"messages_unacknowledged":1,"consumers":1,"durable":true,"arguments":{"x-message-ttl":60000,"x-dead-letter-exchange":"cmd.dead.dlx","x-dead-letter-routing-key":"email.dead"}}
                """;
        QueueView view = RabbitManagementClient.toView(new ObjectMapper().readTree(json));
        assertEquals("q.cmd.email", view.name());
        assertEquals(3, view.messages());
        assertEquals(1, view.consumers());
        assertEquals(60000, view.ttl());
        assertEquals("cmd.dead.dlx", view.deadLetterExchange());
        assertEquals("email.dead", view.deadLetterRoutingKey());
    }

    @Test
    void listaY404VienenDeLaApiHttp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/queues", exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            if (path.endsWith("/q.no.existe")) {
                byte[] body = "{\"error\":\"Object Not Found\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
                return;
            }
            byte[] body = """
                    [{"name":"q.demo.avisos","messages":4,"consumers":2,"durable":true,"arguments":{}}]
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        MqManagementProperties properties = new MqManagementProperties();
        properties.setUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setUsername("barriodigital");
        properties.setPassword("secreto");
        properties.setVhost("/");
        RabbitManagementClient client = new RabbitManagementClient(properties);

        List<QueueView> queues = client.listQueues();
        assertEquals(1, queues.size());
        assertEquals("q.demo.avisos", queues.getFirst().name());
        assertEquals(4, queues.getFirst().messages());
        assertEquals(2, queues.getFirst().consumers());
        assertNull(queues.getFirst().ttl());

        assertThrows(ResourceNotFoundException.class, () -> client.getQueue("q.no.existe"));
    }
}
