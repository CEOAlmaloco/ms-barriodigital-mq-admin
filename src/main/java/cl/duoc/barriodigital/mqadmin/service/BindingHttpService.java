package cl.duoc.barriodigital.mqadmin.service;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.AlreadyExistsException;
import cl.duoc.barriodigital.mqadmin.domain.BindingDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.InvalidArgumentException;
import cl.duoc.barriodigital.mqadmin.domain.ProtectedResourceException;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.web.BindingView;
import cl.duoc.barriodigital.mqadmin.web.ExchangeView;
import org.springframework.stereotype.Service;

@Service
public class BindingHttpService {

    private static final String ROUTING_KEY_MESSAGE = "La routing key no cumple el formato";

    private final RabbitAdminService admin;
    private final RabbitManagementClient management;
    private final MqProtectionProperties protection;

    public BindingHttpService(
            RabbitAdminService admin,
            RabbitManagementClient management,
            MqProtectionProperties protection) {
        this.admin = admin;
        this.management = management;
        this.protection = protection;
    }

    public BindingView create(BindingDeclaration declaration) {
        ExchangeView exchange = requireEndpoints(declaration);
        requireRoutingKey(exchange, declaration.routingKey());
        if (management.hasBinding(declaration)) {
            throw new AlreadyExistsException("El binding ya existe");
        }
        admin.bind(declaration);
        return BindingView.of(declaration);
    }

    /**
     * {@code queue.unbind} no falla si el binding no existe. El 404 sale del Management.
     */
    public void delete(BindingDeclaration declaration) {
        ExchangeView exchange = requireEndpoints(declaration);
        requireRoutingKey(exchange, declaration.routingKey());
        if (!management.hasBinding(declaration)) {
            throw new ResourceNotFoundException("El binding no existe");
        }
        admin.unbind(declaration);
    }

    private ExchangeView requireEndpoints(BindingDeclaration declaration) {
        if (protection.isProtected(declaration.exchange()) || protection.isProtected(declaration.queue())) {
            throw new ProtectedResourceException();
        }
        ExchangeView exchange = management.getExchange(declaration.exchange());
        management.getQueue(declaration.queue());
        return exchange;
    }

    private static void requireRoutingKey(ExchangeView exchange, String routingKey) {
        if ("fanout".equalsIgnoreCase(exchange.type())) {
            return;
        }
        if (routingKey == null || routingKey.isBlank()) {
            throw new InvalidArgumentException(ROUTING_KEY_MESSAGE);
        }
    }
}
