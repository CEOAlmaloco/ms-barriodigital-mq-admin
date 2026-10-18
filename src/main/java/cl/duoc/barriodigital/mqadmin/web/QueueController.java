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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mq/queues")
public class QueueController {

    private final QueueHttpService queues;

    public QueueController(QueueHttpService queues) {
        this.queues = queues;
    }

    @GetMapping
    public List<QueueView> list(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles) {
        AdminCaller.requireAdmin(roles);
        return queues.list();
    }

    @GetMapping("/{name}")
    public QueueView get(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @PathVariable String name) {
        AdminCaller.requireAdmin(roles);
        return queues.get(name);
    }

    @PostMapping
    public ResponseEntity<QueueView> create(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @RequestBody QueueRequest body) {
        AdminCaller.requireAdmin(roles);
        boolean durable = body.durable() == null || body.durable();
        QueueDeclaration declaration = new QueueDeclaration(
                body.name(),
                durable,
                body.ttl(),
                body.deadLetterExchange(),
                body.deadLetterRoutingKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(queues.create(declaration));
    }

    @DeleteMapping("/{name}/messages")
    public ResponseEntity<Void> purge(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @PathVariable String name) {
        AdminCaller.requireAdmin(roles);
        queues.purge(name);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @PathVariable String name) {
        AdminCaller.requireAdmin(roles);
        queues.delete(name);
        return ResponseEntity.noContent().build();
    }
}
