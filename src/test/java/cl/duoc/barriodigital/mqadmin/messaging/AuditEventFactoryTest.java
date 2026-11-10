package cl.duoc.barriodigital.mqadmin.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuditEventFactoryTest {

    private static final Instant NOW = Instant.parse("2026-11-12T15:00:00Z");

    @AfterEach
    void clear() {
        ActorContext.clear();
    }

    @Test
    void armaSobreQueuePurgedConActorYPayload() {
        ActorContext.set(new AuditActor(
                "2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d",
                "Admin Test",
                "Admin",
                "203.0.113.25"));
        AuditEventFactory factory = new AuditEventFactory(Clock.fixed(NOW, ZoneOffset.UTC));

        AuditEnvelope event = factory.create(
                "queue",
                "q.demo.avisos",
                "purged",
                Map.of("messages", 12),
                Map.of("messagesRemoved", 12));

        assertThat(event.eventType()).isEqualTo("queue.purged");
        assertThat(event.schemaVersion()).isEqualTo(1);
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.producer()).isEqualTo("mq-admin");
        assertThat(event.key()).isEqualTo("queue:q.demo.avisos");
        assertThat(event.actor().sourceIp()).isEqualTo("203.0.113.25");
        assertThat(event.payload().before()).isEqualTo(Map.of("messages", 12));
        assertThat(event.payload().after()).isEqualTo(Map.of("messagesRemoved", 12));
    }

    @Test
    void bindingKeyUsaExchangeColaYRoutingKey() {
        ActorContext.set(new AuditActor("u1", "Admin", "Admin", null));
        AuditEventFactory factory = new AuditEventFactory(Clock.fixed(NOW, ZoneOffset.UTC));

        AuditEnvelope event = factory.create(
                "binding",
                "demo.topic/q.demo.avisos/aviso.nuevo",
                "created",
                null,
                Map.of("exchange", "demo.topic"));

        assertThat(event.key()).isEqualTo("binding:demo.topic/q.demo.avisos/aviso.nuevo");
        assertThat(event.payload().before()).isNull();
    }
}
