package cl.duoc.barriodigital.mqadmin.messaging;

/** Envío confirmado a audit.timeline. */
public interface AuditKafkaSender {

    void sendConfirmed(String topic, String key, String json) throws Exception;
}
