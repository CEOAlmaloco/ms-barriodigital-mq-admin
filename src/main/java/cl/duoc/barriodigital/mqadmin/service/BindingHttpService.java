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

/**
 * Orquesta el alta y la baja de bindings.
 * Comprueba en el Management que el exchange y la cola existan antes de crear o borrar el enlace por AMQP.
 */
@Service
public class BindingHttpService {

    private static final String ROUTING_KEY_MESSAGE = "La routing key no cumple el formato";

    private final RabbitAdminService admin;
    private final RabbitManagementClient management;
    private final MqProtectionProperties protection;

    /** Recibe quién declara por AMQP, quién consulta el Management y la lista de nombres protegidos. */
    public BindingHttpService(
            RabbitAdminService admin,
            RabbitManagementClient management,
            MqProtectionProperties protection) {
        this.admin = admin;
        this.management = management;
        this.protection = protection;
    }

    /**
     * Crea el binding por AMQP.
     * Exige exchange y cola existentes, routing key válida y que el enlace no esté ya.
     */
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
     * Borra el binding por AMQP.
     * Si no está, responde que no existe. queue.unbind no falla al quitar un enlace ausente,
     * así que el 404 se decide consultando el Management.
     */
    public void delete(BindingDeclaration declaration) {
        ExchangeView exchange = requireEndpoints(declaration);
        requireRoutingKey(exchange, declaration.routingKey());
        if (!management.hasBinding(declaration)) {
            throw new ResourceNotFoundException("El binding no existe");
        }
        admin.unbind(declaration);
    }

    /**
     * Rechaza el exchange o la cola si están reservados: {@code cmd.*} y {@code q.cmd.*} responden 409.
     * Después exige que ambos existan en el Management y devuelve el exchange para saber si es fanout.
     */
    private ExchangeView requireEndpoints(BindingDeclaration declaration) {
        if (protection.isProtected(declaration.exchange()) || protection.isProtected(declaration.queue())) {
            throw new ProtectedResourceException();
        }
        ExchangeView exchange = management.getExchange(declaration.exchange());
        management.getQueue(declaration.queue());
        return exchange;
    }

    /**
     * En fanout la routing key puede ir vacía.
     * En direct y topic, una routing key vacía es un error 400.
     */
    private static void requireRoutingKey(ExchangeView exchange, String routingKey) {
        if ("fanout".equalsIgnoreCase(exchange.type())) {
            return;
        }
        if (routingKey == null || routingKey.isBlank()) {
            throw new InvalidArgumentException(ROUTING_KEY_MESSAGE);
        }
    }
}
