package cl.duoc.barriodigital.mqadmin.adapter.out.messaging;

/** Envío confirmado a audit.timeline. */
public interface AuditKafkaSender {

    void sendConfirmed(String topic, String key, String json) throws Exception;
}
