package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.QueueDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.QueueStatus;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QueueController.class)
@Import({QueueHttpService.class, MqExceptionHandler.class})
class QueueControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RabbitAdminService admin;

    @MockBean
    private RabbitManagementClient management;

    @MockBean
    private MqProtectionProperties protection;

    @Test
    void sinRolTodosLosEndpointsResponden403() throws Exception {
        mvc.perform(get("/api/mq/queues")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(AdminCaller.FORBIDDEN_MESSAGE));
        mvc.perform(get("/api/mq/queues/q.demo.avisos")).andExpect(status().isForbidden());
        mvc.perform(post("/api/mq/queues").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/mq/queues/q.demo.avisos")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/mq/queues/q.demo.avisos/messages")).andExpect(status().isForbidden());
        verify(admin, never()).deleteQueue(any());
        verify(admin, never()).purgeQueue(any());
    }

    @Test
    void funcionarioResponde403() throws Exception {
        mvc.perform(get("/api/mq/queues").header(AdminCaller.HEADER_USER_ROLES, "Funcionario"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postCreaLaColaYResponde201() throws Exception {
        when(admin.queueInfo("q.demo.avisos")).thenThrow(new ResourceNotFoundException("La cola no existe"));

        mvc.perform(post("/api/mq/queues")
                        .header(AdminCaller.HEADER_USER_ROLES, "Funcionario, Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("q.demo.avisos"))
                .andExpect(jsonPath("$.messages").value(0))
                .andExpect(jsonPath("$.consumers").value(0))
                .andExpect(jsonPath("$.durable").value(true))
                .andExpect(jsonPath("$.ttl").value(60000))
                .andExpect(jsonPath("$.deadLetterExchange").value("cmd.dead.dlx"))
                .andExpect(jsonPath("$.deadLetterRoutingKey").value("avisos.dead"));

        verify(admin).createQueue(new QueueDeclaration(
                "q.demo.avisos", true, 60000, "cmd.dead.dlx", "avisos.dead"));
    }

    @Test
    void getListaNombreMensajesYConsumidores() throws Exception {
        when(management.listQueues()).thenReturn(List.of(
                new QueueView("q.cmd.email", 2, 1, true, null, "cmd.dead.dlx", "email.dead")));

        mvc.perform(get("/api/mq/queues").header(AdminCaller.HEADER_USER_ROLES, "ROLE_Admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("q.cmd.email"))
                .andExpect(jsonPath("$[0].messages").value(2))
                .andExpect(jsonPath("$[0].consumers").value(1));
    }

    @Test
    void deleteResponde204Y404SiNoExiste() throws Exception {
        when(admin.queueInfo("q.demo.avisos")).thenReturn(new QueueStatus("q.demo.avisos", 0, 0));
        mvc.perform(delete("/api/mq/queues/q.demo.avisos").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isNoContent());
        verify(admin).deleteQueue("q.demo.avisos");

        when(admin.queueInfo("q.no.existe")).thenThrow(new ResourceNotFoundException("La cola no existe"));
        mvc.perform(delete("/api/mq/queues/q.no.existe").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La cola no existe"));
        verify(admin, never()).deleteQueue("q.no.existe");
    }

    @Test
    void purgeResponde204Y409EnColaDelSistema() throws Exception {
        when(admin.queueInfo("q.demo.avisos")).thenReturn(new QueueStatus("q.demo.avisos", 0, 0));
        mvc.perform(delete("/api/mq/queues/q.demo.avisos/messages").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isNoContent());
        verify(admin).purgeQueue("q.demo.avisos");

        when(protection.isProtected("q.cmd.email")).thenReturn(true);
        mvc.perform(delete("/api/mq/queues/q.cmd.email/messages").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El nombre está reservado para la topología"));
        verify(admin, never()).purgeQueue("q.cmd.email");
    }

    @Test
    void postDeUnaColaQueYaExisteResponde409() throws Exception {
        when(admin.queueInfo("q.demo.avisos")).thenReturn(
                new cl.duoc.barriodigital.mqadmin.domain.QueueStatus("q.demo.avisos", 1, 0));

        mvc.perform(post("/api/mq/queues")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La cola ya existe"));
        verify(admin, never()).createQueue(any());
    }

    private static String body() {
        return """
                {"name":"q.demo.avisos","ttl":60000,"deadLetterExchange":"cmd.dead.dlx","deadLetterRoutingKey":"avisos.dead"}
                """;
    }
}
