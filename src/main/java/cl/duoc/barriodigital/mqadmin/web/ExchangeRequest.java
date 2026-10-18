package cl.duoc.barriodigital.mqadmin.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Exchange nuevo. durable vale true si se omite. type es direct, topic o fanout.")
public record ExchangeRequest(
        @MqName
        @Schema(example = "demo.topic")
        String name,
        @ExchangeKind
        @Schema(example = "topic", allowableValues = {"direct", "topic", "fanout"})
        String type,
        @Schema(example = "true")
        Boolean durable
) {
}
