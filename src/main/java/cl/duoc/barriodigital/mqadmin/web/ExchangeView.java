package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.ExchangeDeclaration;

/**
 * Respuesta al consultar o crear un exchange.
 * Trae el nombre, el tipo direct, topic o fanout, y si sobrevive a un reinicio.
 */
public record ExchangeView(String name, String type, boolean durable) {

    /** Arma la respuesta de un exchange recién creado, con el tipo ya en minúsculas. */
    public static ExchangeView created(ExchangeDeclaration declaration) {
        return new ExchangeView(declaration.name(), declaration.type().wireName(), declaration.durable());
    }
}
