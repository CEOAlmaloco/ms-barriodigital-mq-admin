package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.service.BindingHttpService;
import cl.duoc.barriodigital.mqadmin.service.ExchangeHttpService;
import cl.duoc.barriodigital.mqadmin.service.QueueHttpService;
import cl.duoc.barriodigital.mqadmin.service.RabbitAdminService;
import cl.duoc.barriodigital.mqadmin.service.RabbitManagementClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {QueueController.class, ExchangeController.class, BindingController.class})
@Import({
        QueueHttpService.class,
        ExchangeHttpService.class,
        BindingHttpService.class,
        MqExceptionHandler.class,
        AdminGateFilter.class
})
class MqInputValidationTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RabbitAdminService admin;

    @MockBean
    private RabbitManagementClient management;

    @MockBean
    private MqProtectionProperties protection;

    @Test
    void sinRolNiSiquieraEvaluaElCuerpo() throws Exception {
        mvc.perform(post("/api/mq/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("esto no es json"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(AdminCaller.FORBIDDEN_MESSAGE))
                .andExpect(jsonPath("$.path").value("/api/mq/queues"));
        verify(admin, never()).createQueue(any());
    }

    @Test
    void nombreVacioConEspaciosOCaracteresInvalidosResponde400() throws Exception {
        expectName("", MqNames.FORMAT);
        expectName("q demo", MqNames.FORMAT);
        expectName("q.demo/avisos", MqNames.FORMAT);
        verify(admin, never()).createQueue(any());
    }

    @Test
    void prefijoAmqResponde400() throws Exception {
        expectName("amq.direct", MqNames.AMQ);
        mvc.perform(delete("/api/mq/exchanges/amq.topic").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MqNames.AMQ));
        mvc.perform(post("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exchange":"amq.topic","queue":"q.demo.avisos","routingKey":"aviso.nuevo"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MqNames.AMQ));
        verify(admin, never()).deleteExchange(any());
        verify(admin, never()).bind(any());
    }

    @Test
    void ttlYDeadLetterInvalidosResponden400() throws Exception {
        expectQueue("""
                {"name":"q.demo.avisos","ttl":0}
                """, MqNames.TTL);
        expectQueue("""
                {"name":"q.demo.avisos","ttl":86400001}
                """, MqNames.TTL);
        expectQueue("""
                {"name":"q.demo.avisos","ttl":"no"}
                """, MqNames.TTL);
        expectQueue("""
                {"name":"q.demo.avisos","durable":"si"}
                """, MqNames.DURABLE);
        expectQueue("""
                {"name":"q.demo.avisos","deadLetterRoutingKey":"avisos.dead"}
                """, MqNames.DLX);
        mvc.perform(post("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exchange":"demo.topic","queue":"q.demo.avisos","routingKey":"aviso nuevo"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MqNames.ROUTING))
                .andExpect(jsonPath("$.path").value("/api/mq/bindings"));
    }

    @Test
    void borrarOVaciarUnRecursoDelSistemaResponde409() throws Exception {
        when(protection.isProtected("q.cmd.email")).thenReturn(true);
        when(protection.isProtected("cmd.topic")).thenReturn(true);

        mvc.perform(delete("/api/mq/queues/q.cmd.email").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El nombre está reservado para la topología"))
                .andExpect(jsonPath("$.path").value("/api/mq/queues/q.cmd.email"));
        mvc.perform(delete("/api/mq/queues/q.cmd.email/messages").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El nombre está reservado para la topología"));
        mvc.perform(delete("/api/mq/exchanges/cmd.topic").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El nombre está reservado para la topología"));

        verify(admin, never()).deleteQueue(any());
        verify(admin, never()).purgeQueue(any());
        verify(admin, never()).deleteExchange(any());
    }

    @Test
    void nombreValidoQueNoExisteSigueSiendo404() throws Exception {
        when(management.getQueue("q.no.existe")).thenThrow(new ResourceNotFoundException("La cola no existe"));

        mvc.perform(get("/api/mq/queues/q.no.existe").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("La cola no existe"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/mq/queues/q.no.existe"));

        mvc.perform(get("/api/mq/queues/q demo").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MqNames.FORMAT));
        verify(management, never()).getQueue("q demo");
    }

    private void expectName(String name, String message) throws Exception {
        expectQueue("{\"name\":\"" + name + "\"}", message);
    }

    private void expectQueue(String json, String message) throws Exception {
        mvc.perform(post("/api/mq/queues")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/mq/queues"));
    }
}
