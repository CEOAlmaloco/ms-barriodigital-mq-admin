package cl.duoc.barriodigital.mqadmin.domain;

/**
 * Datos internos para declarar un exchange por AMQP.
 * Guarda el nombre, el tipo y si debe sobrevivir a un reinicio de RabbitMQ.
 */
public record ExchangeDeclaration(String name, ExchangeType type, boolean durable) {
}
