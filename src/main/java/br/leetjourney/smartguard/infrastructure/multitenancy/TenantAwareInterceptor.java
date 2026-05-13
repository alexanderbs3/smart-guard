package br.leetjourney.smartguard.infrastructure.multitenancy;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantAwareInterceptor implements HandlerInterceptor {

    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        UUID tenantId = TenantContext.getCurrentTenant();

        if (tenantId != null) {
            // Seta o tenant no contexto da sessão PostgreSQL — ativa o RLS
            jdbcTemplate.execute("SET LOCAL app.current_tenant = '" + tenantId + "'");
            log.debug("RLS ativado para tenant: {}", tenantId);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        TenantContext.clear();
    }
}
