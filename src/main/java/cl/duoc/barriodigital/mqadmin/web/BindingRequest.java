package cl.duoc.barriodigital.mqadmin.web;

public record BindingRequest(String exchange, String queue, String routingKey) {
}
