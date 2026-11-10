package cl.duoc.barriodigital.mqadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.AlreadyExistsException;
import cl.duoc.barriodigital.mqadmin.domain.QueueDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.QueueStatus;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.messaging.ActorContext;
import cl.duoc.barriodigital.mqadmin.messaging.AuditActor;
import cl.duoc.barriodigital.mqadmin.messaging.AuditEnvelope;
import cl.duoc.barriodigital.mqadmin.messaging.AuditEventFactory;
import cl.duoc.barriodigital.mqadmin.messaging.AuditPublisher;
import cl.duoc.barriodigital.mqadmin.web.QueueView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QueueHttpServiceAuditTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), ZoneOffset.UTC);

    private final RabbitAdminService admin = mock(RabbitAdminService.class);
    private final RabbitManagementClient management = mock(RabbitManagementClient.class);
    private final MqProtectionProperties protection = mock(MqProtectionProperties.class);
    private final AuditPublisher auditPublisher = mock(AuditPublisher.class);
    private final QueueHttpService service = new QueueHttpService(
            admin, management, protection, new AuditEventFactory(CLOCK), auditPublisher);

    @BeforeEach
    void setActor() {
        ActorContext.set(new AuditActor("u1", "Admin Test", "Admin", "203.0.113.25"));
        when(protection.isProtected(any())).thenReturn(false);
    }

    @AfterEach
    void clear() {
        ActorContext.clear();
    }

    @Test
    void crearPublicaQueueCreated() {
        when(admin.queueInfo("q.demo.avisos")).thenThrow(new ResourceNotFoundException("La cola no existe"));
        QueueDeclaration declaration = new QueueDeclaration("q.demo.avisos", true, null, null, null);

        service.create(declaration);

        ArgumentCaptor<AuditEnvelope> captor = ArgumentCaptor.forClass(AuditEnvelope.class);
        verify(auditPublisher).publish(captor.capture());
        AuditEnvelope event = captor.getValue();
        assertThat(event.eventType()).isEqualTo("queue.created");
        assertThat(event.key()).isEqualTo("queue:q.demo.avisos");
        assertThat(event.payload().before()).isNull();
        assertThat(event.payload().after()).isEqualTo(QueueView.created(declaration));
    }

    @Test
    void vaciarPublicaQueuePurgedConMensajes() {
        when(admin.queueInfo("q.demo.avisos")).thenReturn(new QueueStatus("q.demo.avisos", 12, 1));

        service.purge("q.demo.avisos");

        ArgumentCaptor<AuditEnvelope> captor = ArgumentCaptor.forClass(AuditEnvelope.class);
        verify(auditPublisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("queue.purged");
        assertThat(captor.getValue().payload().before()).isEqualTo(Map.of("messages", 12));
        assertThat(captor.getValue().payload().after()).isEqualTo(Map.of("messagesRemoved", 12));
    }

    @Test
    void borrarPublicaQueueDeleted() {
        when(admin.queueInfo("q.demo.avisos")).thenReturn(new QueueStatus("q.demo.avisos", 0, 0));
        QueueView before = new QueueView("q.demo.avisos", 0, 0, true, null, null, null);
        when(management.getQueue("q.demo.avisos")).thenReturn(before);

        service.delete("q.demo.avisos");

        ArgumentCaptor<AuditEnvelope> captor = ArgumentCaptor.forClass(AuditEnvelope.class);
        verify(auditPublisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("queue.deleted");
        assertThat(captor.getValue().payload().before()).isEqualTo(before);
        assertThat(captor.getValue().payload().after()).isNull();
        verify(admin).deleteQueue(eq("q.demo.avisos"));
    }

    @Test
    void altaDuplicadaNoPublica() {
        when(admin.queueInfo("q.demo.avisos")).thenReturn(new QueueStatus("q.demo.avisos", 0, 0));

        assertThatThrownBy(() ->
                service.create(new QueueDeclaration("q.demo.avisos", true, null, null, null)))
                .isInstanceOf(AlreadyExistsException.class);

        verify(auditPublisher, never()).publish(any());
    }
}
