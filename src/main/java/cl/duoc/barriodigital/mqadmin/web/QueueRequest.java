package cl.duoc.barriodigital.mqadmin.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@DeadLetter
@Schema(description = "Cola nueva. durable vale true si se omite.")
public record QueueRequest(
        @MqName
        @Schema(example = "q.demo.avisos")
        String name,
        @Schema(example = "true")
        Boolean durable,
        @Min(value = 1, message = MqNames.TTL)
        @Max(value = 86_400_000, message = MqNames.TTL)
        @Schema(example = "60000", description = "Milisegundos. Entre 1 y 86400000.")
        Integer ttl,
        @Schema(example = "cmd.dead.dlx")
        String deadLetterExchange,
        @Schema(example = "avisos.dead")
        String deadLetterRoutingKey
) {
}
