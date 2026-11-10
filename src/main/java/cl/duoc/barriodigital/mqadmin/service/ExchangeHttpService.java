package cl.duoc.barriodigital.mqadmin.service;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.AlreadyExistsException;
import cl.duoc.barriodigital.mqadmin.domain.ExchangeDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.ProtectedResourceException;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.messaging.AuditEventFactory;
import cl.duoc.barriodigital.mqadmin.messaging.AuditPublisher;
import cl.duoc.barriodigital.mqadmin.web.ExchangeView;
import org.springframework.stereotype.Service;

/**
 * Orquesta el alta y la baja de exchanges.
 * Rechaza un nombre reservado o uno que el Management ya ve, y solo entonces crea o borra por AMQP.
 * Publica en audit.timeline tras éxito; RabbitAdminService no conoce Kafka.
 */
@Service
public class ExchangeHttpService {

    private static final String RESOURCE = "exchange";

    private final RabbitAdminService admin;
    private final RabbitManagementClient management;
    private final MqProtectionProperties protection;
    private final AuditEventFactory auditEvents;
    private final AuditPublisher auditPublisher;

    public ExchangeHttpService(
            RabbitAdminService admin,
            RabbitManagementClient management,
            MqProtectionProperties protection,
            AuditEventFactory auditEvents,
            AuditPublisher auditPublisher) {
        this.admin = admin;
        this.management = management;
        this.protection = protection;
        this.auditEvents = auditEvents;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Crea el exchange por AMQP si el nombre no está reservado y todavía no existe.
     * Si el Management ya lo ve, lanza "El exchange ya existe".
     */
    public ExchangeView create(ExchangeDeclaration declaration) {
        if (protection.isProtected(declaration.name())) {
            throw new ProtectedResourceException();
        }
        try {
            management.getExchange(declaration.name());
        } catch (ResourceNotFoundException notFound) {
            admin.createExchange(declaration);
            ExchangeView after = ExchangeView.created(declaration);
            auditPublisher.publish(auditEvents.create(RESOURCE, declaration.name(), "created", null, after));
            return after;
        }
        throw new AlreadyExistsException("El exchange ya existe");
    }

    /**
     * Borra el exchange por AMQP.
     * Un nombre {@code cmd.*} o {@code q.cmd.*} responde 409 aunque no exista.
     * El 404 lo decide el Management, porque deleteExchange de Spring solo devuelve false ante un IOException.
     */
    public void delete(String name) {
        if (protection.isProtected(name)) {
            throw new ProtectedResourceException();
        }
        ExchangeView before = management.getExchange(name);
        admin.deleteExchange(name);
        auditPublisher.publish(auditEvents.create(RESOURCE, name, "deleted", before, null));
    }
}
