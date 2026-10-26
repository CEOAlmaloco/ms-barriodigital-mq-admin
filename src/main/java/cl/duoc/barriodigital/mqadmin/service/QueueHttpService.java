package cl.duoc.barriodigital.mqadmin.service;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.AlreadyExistsException;
import cl.duoc.barriodigital.mqadmin.domain.ProtectedResourceException;
import cl.duoc.barriodigital.mqadmin.domain.QueueDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.web.QueueView;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta el listado, el alta, el vaciado y la baja de colas.
 * Listar consulta la API HTTP de Management. Crear y borrar usan AMQP, después de rechazar un nombre reservado.
 */
@Service
public class QueueHttpService {

    private final RabbitAdminService admin;
    private final RabbitManagementClient management;
    private final MqProtectionProperties protection;

    /** Recibe quién opera por AMQP, quién lista por el Management y la lista de nombres protegidos. */
    public QueueHttpService(
            RabbitAdminService admin,
            RabbitManagementClient management,
            MqProtectionProperties protection) {
        this.admin = admin;
        this.management = management;
        this.protection = protection;
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
            return QueueView.created(declaration);
        }
        throw new AlreadyExistsException("La cola ya existe");
    }

    /** Borra la cola por AMQP después de comprobar que no está reservada y que existe. */
    public void delete(String name) {
        requireExisting(name);
        admin.deleteQueue(name);
    }

    /** Vacía los mensajes y deja la cola, con las mismas comprobaciones que borrar. */
    public void purge(String name) {
        requireExisting(name);
        admin.purgeQueue(name);
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
}
