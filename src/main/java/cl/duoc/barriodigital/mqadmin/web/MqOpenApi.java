package cl.duoc.barriodigital.mqadmin.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;

@Configuration
public class MqOpenApi {

    static final String QUEUE_EXAMPLE = """
            {"name":"q.demo.avisos","durable":true,"ttl":60000,"deadLetterExchange":"cmd.dead.dlx","deadLetterRoutingKey":"avisos.dead"}""";
    static final String EXCHANGE_EXAMPLE = """
            {"name":"demo.topic","type":"topic","durable":true}""";
    static final String BINDING_EXAMPLE = """
            {"exchange":"demo.topic","queue":"q.demo.avisos","routingKey":"aviso.nuevo"}""";

    @Bean
    OpenAPI mqAdminApi() {
        return new OpenAPI().info(new Info()
                .title("mq-admin")
                .version("v1")
                .description("Administración de colas, exchanges y bindings. Exige rol Admin en X-User-Roles."));
    }

    @Bean
    OperationCustomizer mqOperations() {
        return (operation, handler) -> {
            String method = handler.getMethod().getName();
            Class<?> type = handler.getBeanType();
            if (type == QueueController.class && "create".equals(method)) {
                example(operation, QUEUE_EXAMPLE);
            } else if (type == ExchangeController.class && "create".equals(method)) {
                example(operation, EXCHANGE_EXAMPLE);
            } else if (type == BindingController.class && "create".equals(method)) {
                example(operation, BINDING_EXAMPLE);
            }
            codes(operation.getResponses(), handler, method);
            successCode(operation.getResponses(), method);
            return operation;
        };
    }

    private static void successCode(ApiResponses responses, String method) {
        ApiResponse ok = responses.remove("200");
        if (ok == null) {
            return;
        }
        if ("create".equals(method)) {
            ok.setDescription("Creado");
            responses.addApiResponse("201", ok);
            return;
        }
        if ("delete".equals(method) || "purge".equals(method)) {
            ok.setDescription("Sin cuerpo");
            responses.addApiResponse("204", ok);
            return;
        }
        responses.addApiResponse("200", ok);
    }

    private static void example(io.swagger.v3.oas.models.Operation operation, String json) {
        if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
            return;
        }
        MediaType media = operation.getRequestBody().getContent().get("application/json");
        if (media != null) {
            media.setExample(json);
        }
    }

    private static void codes(ApiResponses responses, HandlerMethod handler, String method) {
        responses.addApiResponse("403", error(403, AdminCaller.FORBIDDEN_MESSAGE));
        boolean writes = "create".equals(method) || "delete".equals(method) || "purge".equals(method) || "get".equals(method);
        if (writes) {
            responses.addApiResponse("400", error(400, MqNames.FORMAT));
        }
        if ("get".equals(method) || "delete".equals(method) || "purge".equals(method)) {
            String missing = handler.getBeanType() == ExchangeController.class
                    ? "El exchange no existe"
                    : handler.getBeanType() == BindingController.class
                    ? "El binding no existe"
                    : "La cola no existe";
            responses.addApiResponse("404", error(404, missing));
        }
        if ("create".equals(method) || "delete".equals(method) || "purge".equals(method)) {
            responses.addApiResponse("409", error(409, "El nombre está reservado para la topología"));
        }
    }

    private static ApiResponse error(int status, String message) {
        MediaType media = new MediaType();
        media.setExample(Map.of(
                "status", status,
                "error", status == 400 ? "Bad Request" : status == 403 ? "Forbidden" : status == 404 ? "Not Found" : "Conflict",
                "message", message,
                "path", "/api/mq/queues"));
        Content content = new Content().addMediaType("application/json", media);
        return new ApiResponse().description(message).content(content);
    }
}
