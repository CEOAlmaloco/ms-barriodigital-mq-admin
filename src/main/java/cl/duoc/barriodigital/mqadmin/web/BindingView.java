package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.BindingDeclaration;

/**
 * Respuesta al crear un binding.
 * Devuelve el exchange, la cola y la routing key.
 */
public record BindingView(String exchange, String queue, String routingKey) {

    /** Arma la respuesta desde la declaración interna. Si la routing key venía null, responde texto vacío. */
    public static BindingView of(BindingDeclaration declaration) {
        String routingKey = declaration.routingKey() == null ? "" : declaration.routingKey();
        return new BindingView(declaration.exchange(), declaration.queue(), routingKey);
    }
}
