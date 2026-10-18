package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.BindingDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.InvalidArgumentException;
import cl.duoc.barriodigital.mqadmin.service.BindingHttpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mq/bindings")
public class BindingController {

    private final BindingHttpService bindings;

    public BindingController(BindingHttpService bindings) {
        this.bindings = bindings;
    }

    @PostMapping
    public ResponseEntity<BindingView> create(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @RequestBody BindingRequest body) {
        AdminCaller.requireAdmin(roles);
        BindingDeclaration declaration = new BindingDeclaration(body.exchange(), body.queue(), body.routingKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(bindings.create(declaration));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) String queue,
            @RequestParam(required = false) String routingKey) {
        AdminCaller.requireAdmin(roles);
        if (exchange == null || queue == null || routingKey == null) {
            throw new InvalidArgumentException("Faltan exchange, queue o routingKey");
        }
        bindings.delete(new BindingDeclaration(exchange, queue, routingKey));
        return ResponseEntity.noContent().build();
    }
}
