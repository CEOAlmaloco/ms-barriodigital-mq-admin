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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitAdminServiceTest {

    private final AmqpAdmin admin = mock(AmqpAdmin.class);
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        MqProtectionProperties protection = new MqProtectionProperties();
        protection.setProtectedNames(List.of("amq.direct", "amq.fanout", "amq.topic", "amq.headers"));
        protection.setProtectedPrefixes(List.of("amq.", "cmd.", "q.cmd."));
        service = new RabbitAdminService(admin, protection);
    }

    @Test
    void createQueueDeclaraDurableConTtlYDeadLetter() {
        when(admin.getQueueInfo("q.demo.avisos"))
                .thenReturn(new QueueInformation("q.demo.avisos", 0, 0));

        QueueStatus status = service.createQueue(new QueueDeclaration(
                "q.demo.avisos", true, 60_000, "cmd.dead.dlx", "avisos.dead"));

        Queue declared = declaredQueue();
        assertEquals("q.demo.avisos", declared.getName());
        assertTrue(declared.isDurable());
        assertEquals(60_000, declared.getArguments().get("x-message-ttl"));
        assertEquals("cmd.dead.dlx", declared.getArguments().get("x-dead-letter-exchange"));
        assertEquals("avisos.dead", declared.getArguments().get("x-dead-letter-routing-key"));
        assertEquals(0, status.messageCount());
    }

    @Test
    void createQueueConLaMismaConfiguracionNoFalla() {
        when(admin.getQueueInfo("q.demo.avisos"))
                .thenReturn(new QueueInformation("q.demo.avisos", 2, 1));
        QueueDeclaration declaration = new QueueDeclaration("q.demo.avisos", true, null, null, null);

        service.createQueue(declaration);
        QueueStatus again = service.createQueue(declaration);

        assertEquals(2, again.messageCount());
        assertEquals(1, again.consumerCount());
    }

    @Test
    void createQueueConOtraConfiguracionRespondeConflicto() {
        doThrow(broker(406, "PRECONDITION_FAILED")).when(admin).declareQueue(any(Queue.class));

        ConfigurationConflictException ex = assertThrows(ConfigurationConflictException.class,
                () -> service.createQueue(new QueueDeclaration("q.demo.avisos", false, null, null, null)));

        assertEquals("La configuración de cola no coincide con la que ya existe", ex.getMessage());
    }

    @Test
    void noSePuedeBorrarNiVaciarUnRecursoProtegidoAunqueNoExista() {
        assertThrows(ProtectedResourceException.class, () -> service.deleteQueue("q.cmd.email"));
        assertThrows(ProtectedResourceException.class, () -> service.purgeQueue("q.cmd.no-existe"));
        assertThrows(ProtectedResourceException.class, () -> service.deleteExchange("cmd.topic"));
        assertThrows(ProtectedResourceException.class, () -> service.deleteQueue("amq.direct"));

        verify(admin, never()).deleteQueue(anyString());
        verify(admin, never()).purgeQueue(anyString());
        verify(admin, never()).deleteExchange(anyString());
    }

    @Test
    void noSeCreaNiSeEnlazaUnNombreProtegido() {
        assertThrows(ProtectedResourceException.class,
                () -> service.createQueue(new QueueDeclaration("q.cmd.email", true, null, null, null)));
        assertThrows(ProtectedResourceException.class,
                () -> service.createExchange(new ExchangeDeclaration("cmd.direct", ExchangeType.DIRECT, true)));
        assertThrows(ProtectedResourceException.class,
                () -> service.bind(new BindingDeclaration("cmd.topic", "q.demo.avisos", "email.*")));
        assertThrows(ProtectedResourceException.class,
                () -> service.unbind(new BindingDeclaration("demo.topic", "q.cmd.email", "email.*")));

        verify(admin, never()).declareQueue(any(Queue.class));
        verify(admin, never()).declareExchange(any());
        verify(admin, never()).declareBinding(any());
        verify(admin, never()).removeBinding(any());
    }

    @Test
    void deleteQueueInexistenteRespondeQueNoExiste() {
        when(admin.deleteQueue("q.demo.avisos")).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.deleteQueue("q.demo.avisos"));

        assertEquals("La cola no existe", ex.getMessage());
    }

    @Test
    void queueInfoDevuelveMensajesYConsumidores() {
        when(admin.getQueueInfo("q.demo.avisos"))
                .thenReturn(new QueueInformation("q.demo.avisos", 4, 2));

        QueueStatus status = service.queueInfo("q.demo.avisos");

        assertEquals("q.demo.avisos", status.name());
        assertEquals(4, status.messageCount());
        assertEquals(2, status.consumerCount());
    }

    @Test
    void queueInfoInexistenteRespondeQueNoExiste() {
        when(admin.getQueueInfo("q.demo.avisos")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> service.queueInfo("q.demo.avisos"));
    }

    @Test
    void createExchangeTopicYConflictoSiElTipoNoCoincide() {
        ExchangeDeclaration topic = new ExchangeDeclaration("demo.topic", ExchangeType.TOPIC, true);

        service.createExchange(topic);
        Exchange declared = declaredExchange();
        assertInstanceOf(TopicExchange.class, declared);
        assertEquals("demo.topic", declared.getName());
        assertTrue(declared.isDurable());

        doThrow(broker(406, "PRECONDITION_FAILED")).when(admin).declareExchange(any());
        assertThrows(ConfigurationConflictException.class,
                () -> service.createExchange(new ExchangeDeclaration("demo.topic", ExchangeType.DIRECT, true)));
    }

    @Test
    void bindYUnbindUsanElAdmin() {
        BindingDeclaration declaration = new BindingDeclaration("demo.topic", "q.demo.avisos", "aviso.nuevo");

        service.bind(declaration);
        service.unbind(declaration);

        Binding binding = declaredBinding();
        assertEquals("demo.topic", binding.getExchange());
        assertEquals("q.demo.avisos", binding.getDestination());
        assertEquals("aviso.nuevo", binding.getRoutingKey());
        verify(admin).removeBinding(any(Binding.class));
    }

    @Test
    void bindSinColaDiceQueLaColaNoExiste() {
        doThrow(broker(404, "NOT_FOUND")).when(admin).declareBinding(any());
        when(admin.getQueueInfo("q.demo.avisos")).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.bind(new BindingDeclaration("demo.topic", "q.demo.avisos", "aviso.nuevo")));

        assertEquals("La cola no existe", ex.getMessage());
    }

    private Queue declaredQueue() {
        org.mockito.ArgumentCaptor<Queue> captor = org.mockito.ArgumentCaptor.forClass(Queue.class);
        verify(admin).declareQueue(captor.capture());
        return captor.getValue();
    }

    private Exchange declaredExchange() {
        org.mockito.ArgumentCaptor<Exchange> captor = org.mockito.ArgumentCaptor.forClass(Exchange.class);
        verify(admin).declareExchange(captor.capture());
        return captor.getValue();
    }

    private Binding declaredBinding() {
        org.mockito.ArgumentCaptor<Binding> captor = org.mockito.ArgumentCaptor.forClass(Binding.class);
        verify(admin).declareBinding(captor.capture());
        return captor.getValue();
    }

    private static AmqpIOException broker(int code, String text) {
        AMQP.Channel.Close close = new AMQP.Channel.Close.Builder()
                .replyCode(code)
                .replyText(text)
                .classId(50)
                .methodId(10)
                .build();
        return new AmqpIOException("broker", new ShutdownSignalException(false, false, close, null));
    }
}
