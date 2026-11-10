package cl.duoc.barriodigital.mqadmin.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.barriodigital.mqadmin.config.InternalTokenProperties;
import cl.duoc.barriodigital.mqadmin.config.MqProtectionProperties;
import cl.duoc.barriodigital.mqadmin.messaging.AuditEventFactory;
import cl.duoc.barriodigital.mqadmin.messaging.AuditPublisher;
import cl.duoc.barriodigital.mqadmin.service.QueueHttpService;
import cl.duoc.barriodigital.mqadmin.service.RabbitAdminService;
import cl.duoc.barriodigital.mqadmin.service.RabbitManagementClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = QueueController.class)
@Import({
        QueueHttpService.class,
        MqExceptionHandler.class,
        InternalTokenFilter.class,
        AdminGateFilter.class,
        InternalTokenProperties.class
})
class InternalTokenMvcTest {

    private static final String TOKEN = "test-internal-token";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private InternalTokenProperties tokenProperties;

    @MockBean
    private RabbitAdminService admin;

    @MockBean
    private RabbitManagementClient management;

    @MockBean
    private MqProtectionProperties protection;

    @MockBean
    private AuditEventFactory auditEvents;

    @MockBean
    private AuditPublisher auditPublisher;

    @BeforeEach
    void setToken() {
        tokenProperties.setInternalToken(TOKEN);
    }

    @Test
    void sinTokenResponde401YNoLeeRoles() throws Exception {
        mvc.perform(get("/api/mq/queues").header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(InternalTokenFilter.UNAUTHORIZED_MESSAGE))
                .andExpect(jsonPath("$.path").value("/api/mq/queues"));
        verify(management, never()).listQueues();
    }

    @Test
    void tokenInvalidoResponde401AunqueVengaAdmin() throws Exception {
        mvc.perform(get("/api/mq/queues")
                        .header(InternalTokenFilter.HEADER_INTERNAL_TOKEN, "otro")
                        .header(AdminCaller.HEADER_USER_ROLES, "Admin"))
                .andExpect(status().isUnauthorized());
        verify(management, never()).listQueues();
    }

    @Test
    void conTokenSinAdminResponde403AntesDeValidarElCuerpo() throws Exception {
        mvc.perform(post("/api/mq/queues")
                        .header(InternalTokenFilter.HEADER_INTERNAL_TOKEN, TOKEN)
                        .header(AdminCaller.HEADER_USER_ROLES, "Funcionario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("esto no es json"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(AdminCaller.FORBIDDEN_MESSAGE));
        verify(admin, never()).createQueue(any());
    }
}
