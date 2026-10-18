package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.domain.BindingDeclaration;

public record BindingView(String exchange, String queue, String routingKey) {

    public static BindingView of(BindingDeclaration declaration) {
        String routingKey = declaration.routingKey() == null ? "" : declaration.routingKey();
        return new BindingView(declaration.exchange(), declaration.queue(), routingKey);
    }
}
