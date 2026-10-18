package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.domain.BindingDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.ExchangeDeclaration;
import cl.duoc.barriodigital.mqadmin.domain.ExchangeType;
import cl.duoc.barriodigital.mqadmin.domain.ResourceNotFoundException;
import cl.duoc.barriodigital.mqadmin.service.BindingHttpService;
import cl.duoc.barriodigital.mqadmin.service.ExchangeHttpService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ExchangeController.class, BindingController.class})
@Import({ExchangeHttpService.class, BindingHttpService.class, MqExceptionHandler.class})
class ExchangeBindingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RabbitAdminService admin;

    @MockBean
    private RabbitManagementClient management;

    @MockBean
    private MqProtectionProperties protection;

    @Test
    void sinRolResponde403() throws Exception {
        mvc.perform(post("/api/mq/exchanges").contentType(MediaType.APPLICATION_JSON).content(topic()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(AdminCaller.FORBIDDEN_MESSAGE));
        mvc.perform(delete("/api/mq/exchanges/demo.topic")).andExpect(status().isForbidden());
        mvc.perform(post("/api/mq/bindings").contentType(MediaType.APPLICATION_JSON).content(binding()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/mq/bindings")
                        .param("exchange", "demo.topic")
                        .param("queue", "q.demo.avisos")
                        .param("routingKey", "aviso.nuevo"))
                .andExpect(status().isForbidden());
        verify(admin, never()).createExchange(any());
        verify(admin, never()).bind(any());
    }

    @Test
    void postCreaTopicYDirect() throws Exception {
        when(management.getExchange("demo.topic")).thenThrow(new ResourceNotFoundException("El exchange no existe"));
        when(management.getExchange("demo.direct")).thenThrow(new ResourceNotFoundException("El exchange no existe"));

        mvc.perform(post("/api/mq/exchanges")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(topic()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("demo.topic"))
                .andExpect(jsonPath("$.type").value("topic"))
                .andExpect(jsonPath("$.durable").value(true));

        mvc.perform(post("/api/mq/exchanges")
                        .header(AdminCaller.HEADER_USER_ROLES, "Funcionario, Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"demo.direct","type":"direct"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("direct"))
                .andExpect(jsonPath("$.durable").value(true));

        verify(admin).createExchange(new ExchangeDeclaration("demo.topic", ExchangeType.TOPIC, true));
        verify(admin).createExchange(new ExchangeDeclaration("demo.direct", ExchangeType.DIRECT, true));
    }

    @Test
    void tipoFueraDeLaListaResponde400() throws Exception {
        mvc.perform(post("/api/mq/exchanges")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"demo.headers","type":"headers"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExchangeType.INVALID_MESSAGE));
        verify(admin, never()).createExchange(any());
    }

    @Test
    void postDeUnExchangeQueYaExisteOEstaReservadoResponde409() throws Exception {
        when(management.getExchange("demo.topic")).thenReturn(new ExchangeView("demo.topic", "topic", true));
        mvc.perform(post("/api/mq/exchanges")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(topic()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El exchange ya existe"));

        when(protection.isProtected("cmd.topic")).thenReturn(true);
        mvc.perform(post("/api/mq/exchanges")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"cmd.topic","type":"topic"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El nombre está reservado para la topología"));
        verify(admin, never()).createExchange(any());
    }

    @Test
    void deleteExchangeResponde204Y404SiNoExiste() throws Exception {
        when(management.getExchange("demo.topic")).thenReturn(new ExchangeView("demo.topic", "topic", true));
        mvc.perform(delete("/api/mq/exchanges/demo.topic").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isNoContent());
        verify(admin).deleteExchange("demo.topic");

        when(management.getExchange("demo.no.existe")).thenThrow(new ResourceNotFoundException("El exchange no existe"));
        mvc.perform(delete("/api/mq/exchanges/demo.no.existe").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El exchange no existe"));
        verify(admin, never()).deleteExchange("demo.no.existe");
    }

    @Test
    void postCreaElBindingYLoBorraPorQuery() throws Exception {
        ExchangeView topic = new ExchangeView("demo.topic", "topic", true);
        when(management.getExchange("demo.topic")).thenReturn(topic);
        when(management.getQueue("q.demo.avisos")).thenReturn(
                new QueueView("q.demo.avisos", 0, 0, true, null, null, null));
        when(management.hasBinding(new BindingDeclaration("demo.topic", "q.demo.avisos", "aviso.nuevo")))
                .thenReturn(false, true);

        mvc.perform(post("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(binding()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exchange").value("demo.topic"))
                .andExpect(jsonPath("$.queue").value("q.demo.avisos"))
                .andExpect(jsonPath("$.routingKey").value("aviso.nuevo"));
        verify(admin).bind(new BindingDeclaration("demo.topic", "q.demo.avisos", "aviso.nuevo"));

        mvc.perform(delete("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .param("exchange", "demo.topic")
                        .param("queue", "q.demo.avisos")
                        .param("routingKey", "aviso.nuevo"))
                .andExpect(status().isNoContent());
        verify(admin).unbind(new BindingDeclaration("demo.topic", "q.demo.avisos", "aviso.nuevo"));
    }

    @Test
    void bindingSinColaOSinExchangeResponde404() throws Exception {
        when(management.getExchange("demo.topic")).thenReturn(new ExchangeView("demo.topic", "topic", true));
        when(management.getQueue("q.no.existe")).thenThrow(new ResourceNotFoundException("La cola no existe"));

        mvc.perform(post("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exchange":"demo.topic","queue":"q.no.existe","routingKey":"aviso.nuevo"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La cola no existe"));

        when(management.getExchange("demo.no.existe")).thenThrow(new ResourceNotFoundException("El exchange no existe"));
        mvc.perform(post("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exchange":"demo.no.existe","queue":"q.demo.avisos","routingKey":"aviso.nuevo"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El exchange no existe"));
        verify(admin, never()).bind(any());
    }

    @Test
    void bindingRepetidoOReservadoResponde409YElAusente404() throws Exception {
        when(management.getExchange("demo.topic")).thenReturn(new ExchangeView("demo.topic", "topic", true));
        when(management.getQueue("q.demo.avisos")).thenReturn(
                new QueueView("q.demo.avisos", 0, 0, true, null, null, null));
        when(management.hasBinding(any())).thenReturn(true);

        mvc.perform(post("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(binding()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El binding ya existe"));

        when(management.hasBinding(any())).thenReturn(false);
        mvc.perform(delete("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .param("exchange", "demo.topic")
                        .param("queue", "q.demo.avisos")
                        .param("routingKey", "aviso.nuevo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El binding no existe"));

        when(protection.isProtected("q.cmd.email")).thenReturn(true);
        mvc.perform(post("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exchange":"demo.topic","queue":"q.cmd.email","routingKey":"email.admitted"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El nombre está reservado para la topología"));
        verify(admin, never()).bind(any());
        verify(admin, never()).unbind(any());
    }

    @Test
    void routingKeyVaciaEs400EnTopicYSeAceptaEnFanout() throws Exception {
        when(management.getExchange("demo.topic")).thenReturn(new ExchangeView("demo.topic", "topic", true));
        when(management.getExchange("demo.fanout")).thenReturn(new ExchangeView("demo.fanout", "fanout", true));
        when(management.getQueue("q.demo.avisos")).thenReturn(
                new QueueView("q.demo.avisos", 0, 0, true, null, null, null));

        mvc.perform(post("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exchange":"demo.topic","queue":"q.demo.avisos","routingKey":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La routing key no cumple el formato"));

        mvc.perform(post("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exchange":"demo.fanout","queue":"q.demo.avisos","routingKey":""}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routingKey").value(""));
        verify(admin).bind(new BindingDeclaration("demo.fanout", "q.demo.avisos", ""));
    }

    @Test
    void deleteSinQueryResponde400() throws Exception {
        mvc.perform(delete("/api/mq/bindings")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin")
                        .param("exchange", "demo.topic")
                        .param("queue", "q.demo.avisos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Faltan exchange, queue o routingKey"));
        verify(admin, never()).unbind(any());
    }

    private static String topic() {
        return """
                {"name":"demo.topic","type":"topic","durable":true}
                """;
    }

    private static String binding() {
        return """
                {"exchange":"demo.topic","queue":"q.demo.avisos","routingKey":"aviso.nuevo"}
                """;
    }
}
