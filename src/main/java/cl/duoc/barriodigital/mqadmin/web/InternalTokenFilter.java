package cl.duoc.barriodigital.mqadmin.web;

import cl.duoc.barriodigital.mqadmin.config.InternalTokenProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Valida {@code X-Internal-Token} antes de {@link AdminGateFilter}.
 * Sin token válido responde 401 y no lee roles ni el cuerpo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";
    public static final String UNAUTHORIZED_MESSAGE = "Token interno invalido";

    private final InternalTokenProperties tokenProperties;
    private final ObjectMapper mapper;

    public InternalTokenFilter(InternalTokenProperties tokenProperties, ObjectMapper mapper) {
        this.tokenProperties = tokenProperties;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/mq/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        if (!tokenValid(request.getHeader(HEADER_INTERNAL_TOKEN))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(
                    response.getOutputStream(),
                    MqErrorBody.of(401, UNAUTHORIZED_MESSAGE, request.getRequestURI())
            );
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean tokenValid(String presented) {
        String expected = tokenProperties.getInternalToken();
        return expected != null && !expected.isBlank() && expected.equals(presented);
    }
}
