package cl.duoc.barriodigital.mqadmin.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cuerpo para crear un binding.
 * Indica el exchange, la cola y la routing key.
 * Si el exchange es fanout, la routing key puede ir vacía, porque fanout entrega a todas las colas enlazadas.
 */
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
