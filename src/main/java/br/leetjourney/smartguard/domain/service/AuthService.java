package br.leetjourney.smartguard.domain.service;


import br.leetjourney.smartguard.application.dto.AuthRequest;
import br.leetjourney.smartguard.application.dto.AuthResponse;
import br.leetjourney.smartguard.domain.model.Tenant;
import br.leetjourney.smartguard.domain.model.User;
import br.leetjourney.smartguard.domain.repository.TenantRepository;
import br.leetjourney.smartguard.domain.repository.UserRepository;

import br.leetjourney.smartguard.interfaces.exception.BusinessException;
import br.leetjourney.smartguard.interfaces.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor

public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;



    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Transactional
    public AuthResponse register(AuthRequest.Register request) {
        if (tenantRepository.existsBySlug(request.tenantSlug())) {
            throw new BusinessException("Tenant slug já está em uso: " + request.tenantSlug());
        }

        Tenant tenant = tenantRepository.save(
                Tenant.builder()
                        .name(request.tenantName())
                        .slug(request.tenantSlug())
                        .plan(Tenant.TenantPlan.STARTER)
                        .build()
        );

        User admin = userRepository.save(
                User.builder()
                        .tenant(tenant)
                        .fullName(request.fullName())
                        .email(request.email())
                        .passwordHash(passwordEncoder.encode(request.password()))
                        .role(User.UserRole.TENANT_ADMIN)
                        .build()
        );

        log.info("Novo tenant registrado: {} | admin: {}", tenant.getSlug(), admin.getEmail());

        String token = jwtService.generateToken(admin);
        return AuthResponse.of(token, expirationMs, admin);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest.Login request) {
        Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado: " + request.tenantSlug()));

        User user = userRepository.findByEmailAndTenantId(request.email(), tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Delega validação de senha ao Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getId().toString(), request.password())
        );

        log.info("Login: tenant={} email={}", tenant.getSlug(), user.getEmail());

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, expirationMs, user);
    }
}