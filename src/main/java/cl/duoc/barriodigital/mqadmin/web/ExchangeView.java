package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.ExchangeDeclaration;

public record ExchangeView(String name, String type, boolean durable) {

    public static ExchangeView created(ExchangeDeclaration declaration) {
        return new ExchangeView(declaration.name(), declaration.type().wireName(), declaration.durable());
    }
}
