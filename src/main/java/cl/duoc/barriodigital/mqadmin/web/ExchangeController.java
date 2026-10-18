package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.ExchangeDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.ExchangeType;
import cl.duoc.barriodigital.mqadmin.service.ExchangeHttpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mq/exchanges")
public class ExchangeController {

    private final ExchangeHttpService exchanges;

    public ExchangeController(ExchangeHttpService exchanges) {
        this.exchanges = exchanges;
    }

    @PostMapping
    public ResponseEntity<ExchangeView> create(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @RequestBody ExchangeRequest body) {
        AdminCaller.requireAdmin(roles);
        boolean durable = body.durable() == null || body.durable();
        ExchangeDeclaration declaration = new ExchangeDeclaration(
                body.name(),
                ExchangeType.parse(body.type()),
                durable);
        return ResponseEntity.status(HttpStatus.CREATED).body(exchanges.create(declaration));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = AdminCaller.HEADER_USER_ROLES, required = false) String roles,
            @PathVariable String name) {
        AdminCaller.requireAdmin(roles);
        exchanges.delete(name);
        return ResponseEntity.noContent().build();
    }
}
