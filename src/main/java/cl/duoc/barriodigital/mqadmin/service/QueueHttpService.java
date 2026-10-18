package cl.duoc.barriodigital.mqadmin.service;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.AlreadyExistsException;
import cl.duoc.barriodigital.mqadmin.domain.ProtectedResourceException;
import cl.duoc.barriodigital.mqadmin.domain.QueueDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.web.QueueView;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueueHttpService {

    private final RabbitAdminService admin;
    private final RabbitManagementClient management;
    private final MqProtectionProperties protection;

    public QueueHttpService(
            RabbitAdminService admin,
            RabbitManagementClient management,
            MqProtectionProperties protection) {
        this.admin = admin;
        this.management = management;
        this.protection = protection;
    }

    public List<QueueView> list() {
        return management.listQueues();
    }

    public QueueView get(String name) {
        return management.getQueue(name);
    }

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

    public void delete(String name) {
        requireExisting(name);
        admin.deleteQueue(name);
    }

    public void purge(String name) {
        requireExisting(name);
        admin.purgeQueue(name);
    }

    /**
     * {@code deleteQueue} de Spring responde éxito aunque la cola no exista.
     * El 404 se decide antes, y un nombre reservado sigue siendo 409 aunque falte.
     */
    private void requireExisting(String name) {
        if (protection.isProtected(name)) {
            throw new ProtectedResourceException();
        }
        admin.queueInfo(name);
    }
}
