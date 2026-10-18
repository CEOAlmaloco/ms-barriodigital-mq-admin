package cl.duoc.barriodigital.mqadmin.service;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.AlreadyExistsException;
import cl.duoc.barriodigital.mqadmin.domain.ExchangeDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.ProtectedResourceException;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.web.ExchangeView;
import org.springframework.stereotype.Service;

@Service
public class ExchangeHttpService {

    private final RabbitAdminService admin;
    private final RabbitManagementClient management;
    private final MqProtectionProperties protection;

    public ExchangeHttpService(
            RabbitAdminService admin,
            RabbitManagementClient management,
            MqProtectionProperties protection) {
        this.admin = admin;
        this.management = management;
        this.protection = protection;
    }

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
     * {@code deleteExchange} de Spring solo devuelve false ante un {@code IOException}.
     * El 404 se decide con el Management, y un nombre reservado sigue siendo 409 aunque falte.
     */
    public void delete(String name) {
        if (protection.isProtected(name)) {
            throw new ProtectedResourceException();
        }
        management.getExchange(name);
        admin.deleteExchange(name);
    }
}
