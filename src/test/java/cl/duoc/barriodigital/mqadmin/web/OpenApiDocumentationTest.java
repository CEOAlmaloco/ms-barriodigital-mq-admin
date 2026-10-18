package cl.duoc.barriodigital.mqadmin.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void laSpecListaEndpointsEjemplosYCodigos() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/mq/queues'].get").exists())
                .andExpect(jsonPath("$.paths['/api/mq/queues'].post").exists())
                .andExpect(jsonPath("$.paths['/api/mq/queues/{name}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/mq/queues/{name}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/mq/queues/{name}/messages'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/mq/exchanges'].post").exists())
                .andExpect(jsonPath("$.paths['/api/mq/exchanges/{name}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/mq/bindings'].post").exists())
                .andExpect(jsonPath("$.paths['/api/mq/bindings'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/mq/queues'].post.requestBody.content['application/json'].example")
                        .value(MqOpenApi.QUEUE_EXAMPLE))
                .andExpect(jsonPath("$.paths['/api/mq/exchanges'].post.requestBody.content['application/json'].example")
                        .value(MqOpenApi.EXCHANGE_EXAMPLE))
                .andExpect(jsonPath("$.paths['/api/mq/bindings'].post.requestBody.content['application/json'].example")
                        .value(MqOpenApi.BINDING_EXAMPLE))
                .andExpect(jsonPath("$.paths['/api/mq/queues'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/mq/queues/{name}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/mq/queues'].post.responses['400'].description")
                        .value(MqNames.FORMAT))
                .andExpect(jsonPath("$.paths['/api/mq/queues'].get.responses['403'].description")
                        .value(AdminCaller.FORBIDDEN_MESSAGE))
                .andExpect(jsonPath("$.paths['/api/mq/queues/{name}/messages'].delete.responses['409'].description")
                        .value("El nombre está reservado para la topología"));

        MvcResult ui = mvc.perform(get("/swagger-ui.html")).andReturn();
        int status = ui.getResponse().getStatus();
        assertTrue(status == 200 || status == 302 || status == 301, "swagger-ui.html respondio " + status);
        if (status == 302 || status == 301) {
            String location = ui.getResponse().getHeader("Location");
            String path = location == null ? "" : location.replaceFirst("https?://[^/]+", "");
            mvc.perform(get(path)).andExpect(status().isOk());
        }
    }
}
