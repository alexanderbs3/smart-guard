package br.leetjourney.smartguard.infrastructure.multitenancy;

import java.util.UUID;


/**
 * Armazena o tenantId da requisição corrente usando ThreadLocal.
 * É populado pelo JwtAuthenticationFilter após validar o token.
 * O TenantAwareInterceptor usa esse valor para setar app.current_tenant no PostgreSQL,
 * ativando o Row-Level Security.
 */

public class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentTenant(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear(){
        CURRENT_TENANT.remove();
    }

}
