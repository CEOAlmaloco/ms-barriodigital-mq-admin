package cl.duoc.barriodigital.mqadmin.service;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.BindingDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.ConfigurationConflictException;
import cl.duoc.barriodigital.mqadmin.domain.ExchangeDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.ExchangeType;
import cl.duoc.barriodigital.mqadmin.domain.ProtectedResourceException;
import cl.duoc.barriodigital.mqadmin.domain.QueueDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.QueueStatus;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ShutdownSignalException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RabbitAdminService {

    private static final int NOT_FOUND = 404;
    private static final int PRECONDITION_FAILED = 406;

    private final AmqpAdmin admin;
    private final MqProtectionProperties protection;

    public RabbitAdminService(AmqpAdmin admin, MqProtectionProperties protection) {
        this.admin = admin;
        this.protection = protection;
    }

    public QueueStatus createQueue(QueueDeclaration declaration) {
        rejectIfProtected(declaration.name());
        try {
            admin.declareQueue(toQueue(declaration));
        } catch (RuntimeException ex) {
            throw translated(ex, "cola");
        }
        return readQueue(declaration.name());
    }

    public void deleteQueue(String name) {
        rejectIfProtected(name);
        try {
            if (!admin.deleteQueue(name)) {
                throw new ResourceNotFoundException("La cola no existe");
            }
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw translated(ex, "cola");
        }
    }

    public void purgeQueue(String name) {
        rejectIfProtected(name);
        try {
            admin.purgeQueue(name);
        } catch (RuntimeException ex) {
            throw translated(ex, "cola");
        }
    }

    public ExchangeDeclaration createExchange(ExchangeDeclaration declaration) {
        rejectIfProtected(declaration.name());
        try {
            admin.declareExchange(toExchange(declaration));
        } catch (RuntimeException ex) {
            throw translated(ex, "exchange");
        }
        return declaration;
    }

    public void deleteExchange(String name) {
        rejectIfProtected(name);
        try {
            if (!admin.deleteExchange(name)) {
                throw new ResourceNotFoundException("El exchange no existe");
            }
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw translated(ex, "exchange");
        }
    }

    public void bind(BindingDeclaration declaration) {
        rejectIfProtected(declaration.exchange());
        rejectIfProtected(declaration.queue());
        try {
            admin.declareBinding(toBinding(declaration));
        } catch (RuntimeException ex) {
            if (replyCode(ex) == NOT_FOUND) {
                throw missingEndpoint(declaration.queue());
            }
            throw translated(ex, "binding");
        }
    }

    public void unbind(BindingDeclaration declaration) {
        rejectIfProtected(declaration.exchange());
        rejectIfProtected(declaration.queue());
        try {
            admin.removeBinding(toBinding(declaration));
        } catch (RuntimeException ex) {
            if (replyCode(ex) == NOT_FOUND) {
                throw new ResourceNotFoundException("El binding no existe");
            }
            throw translated(ex, "binding");
        }
    }

    public QueueStatus queueInfo(String name) {
        return readQueue(name);
    }

    private QueueStatus readQueue(String name) {
        QueueInformation info = admin.getQueueInfo(name);
        if (info == null) {
            throw new ResourceNotFoundException("La cola no existe");
        }
        return new QueueStatus(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    private ResourceNotFoundException missingEndpoint(String queue) {
        if (admin.getQueueInfo(queue) == null) {
            return new ResourceNotFoundException("La cola no existe");
        }
        return new ResourceNotFoundException("El exchange no existe");
    }

    private void rejectIfProtected(String name) {
        if (protection.isProtected(name)) {
            throw new ProtectedResourceException();
        }
    }

    private RuntimeException translated(RuntimeException ex, String resource) {
        Integer code = replyCode(ex);
        if (code != null && code == PRECONDITION_FAILED) {
            return new ConfigurationConflictException(resource);
        }
        if (code != null && code == NOT_FOUND) {
            return new ResourceNotFoundException(notFoundMessage(resource));
        }
        return ex;
    }

    private static String notFoundMessage(String resource) {
        return switch (resource) {
            case "exchange" -> "El exchange no existe";
            case "binding" -> "El binding no existe";
            default -> "La cola no existe";
        };
    }

    private static Integer replyCode(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ShutdownSignalException shutdown
                    && shutdown.getReason() instanceof AMQP.Channel.Close close) {
                return close.getReplyCode();
            }
            current = current.getCause();
        }
        return null;
    }

    private static Queue toQueue(QueueDeclaration declaration) {
        Map<String, Object> arguments = new HashMap<>();
        if (declaration.ttlMillis() != null) {
            arguments.put("x-message-ttl", declaration.ttlMillis());
        }
        if (declaration.deadLetterExchange() != null) {
            arguments.put("x-dead-letter-exchange", declaration.deadLetterExchange());
        }
        if (declaration.deadLetterRoutingKey() != null) {
            arguments.put("x-dead-letter-routing-key", declaration.deadLetterRoutingKey());
        }
        return new Queue(declaration.name(), declaration.durable(), false, false, arguments);
    }

    private static Exchange toExchange(ExchangeDeclaration declaration) {
        return switch (declaration.type()) {
            case DIRECT -> new DirectExchange(declaration.name(), declaration.durable(), false);
            case TOPIC -> new TopicExchange(declaration.name(), declaration.durable(), false);
            case FANOUT -> new FanoutExchange(declaration.name(), declaration.durable(), false);
        };
    }

    private static Binding toBinding(BindingDeclaration declaration) {
        String routingKey = declaration.routingKey() == null ? "" : declaration.routingKey();
        return new Binding(
                declaration.queue(),
                Binding.DestinationType.QUEUE,
                declaration.exchange(),
                routingKey,
                Map.of());
    }
}
