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

/**
 * Crea, borra, vacía y enlaza recursos por AMQP.
 * No lista el vhost: RabbitMQ solo publica ese inventario en la API HTTP de Management.
 */
@Service
public class RabbitAdminService {

    private static final int NOT_FOUND = 404;
    private static final int PRECONDITION_FAILED = 406;

    private final AmqpAdmin admin;
    private final MqProtectionProperties protection;

    /** Recibe el cliente AMQP de Spring y la lista de nombres protegidos. */
    public RabbitAdminService(AmqpAdmin admin, MqProtectionProperties protection) {
        this.admin = admin;
        this.protection = protection;
    }

    /** Declara la cola por AMQP. Si el nombre está reservado, no la crea. Devuelve mensajes y consumidores. */
    public QueueStatus createQueue(QueueDeclaration declaration) {
        rejectIfProtected(declaration.name());
        try {
            admin.declareQueue(toQueue(declaration));
        } catch (RuntimeException ex) {
            throw translated(ex, "cola");
        }
        return readQueue(declaration.name());
    }

    /** Borra la cola por AMQP. Si RabbitMQ indica que no estaba, lanza "La cola no existe". */
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

    /** Vacía los mensajes y deja la cola declarada. */
    public void purgeQueue(String name) {
        rejectIfProtected(name);
        try {
            admin.purgeQueue(name);
        } catch (RuntimeException ex) {
            throw translated(ex, "cola");
        }
    }

    /** Declara el exchange del tipo pedido. autoDelete queda en false para que no desaparezca al quedarse sin colas. */
    public ExchangeDeclaration createExchange(ExchangeDeclaration declaration) {
        rejectIfProtected(declaration.name());
        try {
            admin.declareExchange(toExchange(declaration));
        } catch (RuntimeException ex) {
            throw translated(ex, "exchange");
        }
        return declaration;
    }

    /** Borra el exchange por AMQP. Si no estaba, lanza "El exchange no existe". */
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

    /** Declara el binding por AMQP. Si falta la cola o el exchange, indica cuál de los dos. */
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

    /** Quita el binding por AMQP. Si no estaba, lanza "El binding no existe". */
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

    /** Lee nombre, cantidad de mensajes y cantidad de consumidores por AMQP. */
    public QueueStatus queueInfo(String name) {
        return readQueue(name);
    }

    /** Lee la cola. Si RabbitMQ no la conoce, lanza "La cola no existe". */
    private QueueStatus readQueue(String name) {
        QueueInformation info = admin.getQueueInfo(name);
        if (info == null) {
            throw new ResourceNotFoundException("La cola no existe");
        }
        return new QueueStatus(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    /** Cuando falla un binding, distingue "La cola no existe" de "El exchange no existe". */
    private ResourceNotFoundException missingEndpoint(String queue) {
        if (admin.getQueueInfo(queue) == null) {
            return new ResourceNotFoundException("La cola no existe");
        }
        return new ResourceNotFoundException("El exchange no existe");
    }

    /** Lanza la excepción de nombre reservado si la protección incluye ese nombre. La capa HTTP responde 409. */
    private void rejectIfProtected(String name) {
        if (protection.isProtected(name)) {
            throw new ProtectedResourceException();
        }
    }

    /**
     * Traduce el cierre del canal AMQP.
     * El código 406 significa que el nombre existe con otras propiedades. El código 404 significa que no existe.
     * Cualquier otro error se propaga sin cambiarlo.
     */
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

    /** Elige la frase "no existe" según se trate de cola, exchange o binding. */
    private static String notFoundMessage(String resource) {
        return switch (resource) {
            case "exchange" -> "El exchange no existe";
            case "binding" -> "El binding no existe";
            default -> "La cola no existe";
        };
    }

    /** Recorre las causas hasta encontrar el código con el que RabbitMQ cerró el canal. */
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

    /**
     * Arma la cola de Spring para declararla por AMQP.
     * durable sigue la declaración; exclusive y autoDelete quedan en false.
     * Si hay TTL o dead-letter, los envía como x-message-ttl y x-dead-letter-*.
     */
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

    /** Elige DirectExchange, TopicExchange o FanoutExchange según la declaración. */
    private static Exchange toExchange(ExchangeDeclaration declaration) {
        return switch (declaration.type()) {
            case DIRECT -> new DirectExchange(declaration.name(), declaration.durable(), false);
            case TOPIC -> new TopicExchange(declaration.name(), declaration.durable(), false);
            case FANOUT -> new FanoutExchange(declaration.name(), declaration.durable(), false);
        };
    }

    /** Arma el binding hacia una cola. Si la routing key viene null, usa texto vacío. */
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
