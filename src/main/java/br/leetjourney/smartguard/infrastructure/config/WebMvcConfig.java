package br.leetjourney.smartguard.infrastructure.config;

import br.leetjourney.smartguard.infrastructure.multitenancy.TenantAwareInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantAwareInterceptor tenantAwareInterceptor;

    public WebMvcConfig(TenantAwareInterceptor tenantAwareInterceptor) {
        this.tenantAwareInterceptor = tenantAwareInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantAwareInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/auth/**", "/api/v1/health");
    }
}
