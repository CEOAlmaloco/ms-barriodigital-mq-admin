package cl.duoc.barriodigital.mqadmin.service;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.AlreadyExistsException;
import cl.duoc.barriodigital.mqadmin.domain.ProtectedResourceException;
import cl.duoc.barriodigital.mqadmin.domain.QueueDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.QueueStatus;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.messaging.AuditEventFactory;
import cl.duoc.barriodigital.mqadmin.messaging.AuditPublisher;
import cl.duoc.barriodigital.mqadmin.web.QueueView;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Orquesta el listado, el alta, el vaciado y la baja de colas.
 * Listar consulta la API HTTP de Management. Crear y borrar usan AMQP, después de rechazar un nombre reservado.
 * Publica en audit.timeline tras éxito; RabbitAdminService no conoce Kafka.
 */
@Service
public class QueueHttpService {

    private static final String RESOURCE = "queue";

    private final RabbitAdminService admin;
    private final RabbitManagementClient management;
    private final MqProtectionProperties protection;
    private final AuditEventFactory auditEvents;
    private final AuditPublisher auditPublisher;

    public QueueHttpService(
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

    /** Pide al Management la lista de colas del vhost. */
    public List<QueueView> list() {
        return management.listQueues();
    }

    /** Pide al Management una cola. Si no está, el cliente lanza "La cola no existe". */
    public QueueView get(String name) {
        return management.getQueue(name);
    }

    /**
     * Crea la cola por AMQP solo si el nombre no está reservado y todavía no existe.
     * Si ya existe, lanza "La cola ya existe".
     */
    public QueueView create(QueueDeclaration declaration) {
        if (protection.isProtected(declaration.name())) {
            throw new ProtectedResourceException();
        }
        try {
            admin.queueInfo(declaration.name());
        } catch (ResourceNotFoundException notFound) {
            admin.createQueue(declaration);
            QueueView after = QueueView.created(declaration);
            auditPublisher.publish(auditEvents.create(RESOURCE, declaration.name(), "created", null, after));
            return after;
        }
        throw new AlreadyExistsException("La cola ya existe");
    }

    /** Borra la cola por AMQP después de comprobar que no está reservada y que existe. */
    public void delete(String name) {
        QueueView before = requireExistingView(name);
        admin.deleteQueue(name);
        auditPublisher.publish(auditEvents.create(RESOURCE, name, "deleted", before, null));
    }

    /** Vacía los mensajes y deja la cola, con las mismas comprobaciones que borrar. */
    public void purge(String name) {
        requireExisting(name);
        QueueStatus status = admin.queueInfo(name);
        int messages = status.messageCount();
        admin.purgeQueue(name);
        auditPublisher.publish(auditEvents.create(
                RESOURCE,
                name,
                "purged",
                Map.of("messages", messages),
                Map.of("messagesRemoved", messages)
        ));
    }

    /**
     * Comprueba el nombre antes de borrar o vaciar.
     * Un nombre {@code cmd.*} o {@code q.cmd.*} responde 409 aunque la cola no exista.
     * Hace falta porque deleteQueue de Spring indica éxito al borrar una cola que no existía.
     */
    private void requireExisting(String name) {
        if (protection.isProtected(name)) {
            throw new ProtectedResourceException();
        }
        admin.queueInfo(name);
    }

    private QueueView requireExistingView(String name) {
        if (protection.isProtected(name)) {
            throw new ProtectedResourceException();
        }
        admin.queueInfo(name);
        return management.getQueue(name);
    }
}
