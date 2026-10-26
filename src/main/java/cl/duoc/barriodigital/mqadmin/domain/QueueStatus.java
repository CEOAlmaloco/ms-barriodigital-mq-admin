package cl.duoc.barriodigital.mqadmin.domain;

/**
 * Estado breve de una cola leído por AMQP.
 * Trae el nombre, cuántos mensajes hay y cuántos consumidores la atienden.
 */
public record QueueStatus(String name, int messageCount, int consumerCount) {
}
