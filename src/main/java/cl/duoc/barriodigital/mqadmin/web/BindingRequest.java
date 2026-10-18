package cl.duoc.barriodigital.mqadmin.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Enlace entre un exchange y una cola. routingKey vacía solo vale en fanout.")
public record BindingRequest(
        @MqName
        @Schema(example = "demo.topic")
        String exchange,
        @MqName
        @Schema(example = "q.demo.avisos")
        String queue,
        @RoutingKey
        @Schema(example = "aviso.nuevo")
        String routingKey
) {
}
