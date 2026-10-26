package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.QueueDeclaration;
import cl.duoc.barriodigital.mqadmin.service.QueueHttpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP de colas.
 * Lista, consulta, crea, vacía o borra una cola. Solo admite el rol Admin.
 * El listado sale de la API HTTP de Management; crear y borrar usan AMQP.
 */
@RestController
@RequestMapping("/api/mq/queues")
@Tag(name = "Colas")
public class QueueController {

    private final QueueHttpService queues;

    /** Recibe el servicio que lista, crea, vacía y borra colas. */
    public QueueController(QueueHttpService queues) {
        this.queues = queues;
    }

    /** GET /api/mq/queues. Devuelve las colas del vhost que publica el Management. */
    @GetMapping
    public List<QueueView> list(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles) {
        AdminCaller.requireAdmin(roles);
        return queues.list();
    }

    /** GET /api/mq/queues/{name}. Devuelve una cola. Si el nombre es inválido, responde 400. */
    @GetMapping("/{name}")
    public QueueView get(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @PathVariable String name) {
        AdminCaller.requireAdmin(roles);
        MqPayloadValidator.requireName(name);
        return queues.get(name);
    }

    /**
     * POST /api/mq/queues. Crea la cola por AMQP y responde 201.
     * Si durable no viene en el JSON, se declara durable.
     */
    @PostMapping
    public ResponseEntity<QueueView> create(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @RequestBody QueueRequest body) {
        AdminCaller.requireAdmin(roles);
        MqPayloadValidator.check(body);
        boolean durable = body.durable() == null || body.durable();
        QueueDeclaration declaration = new QueueDeclaration(
                body.name(),
                durable,
                body.ttl(),
                body.deadLetterExchange(),
                body.deadLetterRoutingKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(queues.create(declaration));
    }

    /** DELETE /api/mq/queues/{name}/messages. Vacía la cola y responde 204, sin cuerpo. */
    @DeleteMapping("/{name}/messages")
    public ResponseEntity<Void> purge(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @PathVariable String name) {
        AdminCaller.requireAdmin(roles);
        MqPayloadValidator.requireName(name);
        queues.purge(name);
        return ResponseEntity.noContent().build();
    }

    /** DELETE /api/mq/queues/{name}. Borra la cola por AMQP y responde 204. */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @PathVariable String name) {
        AdminCaller.requireAdmin(roles);
        MqPayloadValidator.requireName(name);
        queues.delete(name);
        return ResponseEntity.noContent().build();
    }
}
