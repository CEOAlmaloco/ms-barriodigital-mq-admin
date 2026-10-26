package cl.duoc.barriodigital.mqadmin.service;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.AlreadyExistsException;
import cl.duoc.barriodigital.mqadmin.domain.ExchangeDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.ProtectedResourceException;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.web.ExchangeView;
import org.springframework.stereotype.Service;

/**
 * Orquesta el alta y la baja de exchanges.
 * Rechaza un nombre reservado o uno que el Management ya ve, y solo entonces crea o borra por AMQP.
 */
@Service
public class ExchangeHttpService {

    private final RabbitAdminService admin;
    private final RabbitManagementClient management;
    private final MqProtectionProperties protection;

    /** Recibe quién declara por AMQP, quién consulta el Management y la lista de nombres protegidos. */
    public ExchangeHttpService(
            RabbitAdminService admin,
            RabbitManagementClient management,
            MqProtectionProperties protection) {
        this.admin = admin;
        this.management = management;
        this.protection = protection;
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
            return ExchangeView.created(declaration);
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
        management.getExchange(name);
        admin.deleteExchange(name);
    }
}
