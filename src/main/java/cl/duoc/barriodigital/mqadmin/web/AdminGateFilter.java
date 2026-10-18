package cl.duoc.barriodigital.mqadmin.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * El rol se resuelve antes de leer el cuerpo. Un JSON inválido sin Admin responde 403, no 400.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminGateFilter extends OncePerRequestFilter {

    private final ObjectMapper mapper;

    public AdminGateFilter(ObjectMapper mapper) {
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
            FilterChain chain) throws ServletException, IOException {
        if (!AdminCaller.isAdmin(request.getHeader(AdminCaller.HEADER_USER_ROLES))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(),
                    MqErrorBody.of(403, AdminCaller.FORBIDDEN_MESSAGE, request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }
}
