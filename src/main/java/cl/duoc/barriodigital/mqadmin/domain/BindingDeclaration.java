package cl.duoc.barriodigital.mqadmin.domain;

/**
 * Datos internos para declarar o quitar un binding.
 * Guarda el exchange, la cola y la routing key ya validados.
 */
public record BindingDeclaration(String exchange, String queue, String routingKey) {
}
