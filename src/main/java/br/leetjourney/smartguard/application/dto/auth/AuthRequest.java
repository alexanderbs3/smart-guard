package br.leetjourney.smartguard.application.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public sealed interface AuthRequest permits AuthRequest.Register, AuthRequest.Login{

    record Register(
            @NotBlank @Size(min = 3, max = 100)
            String tenantName,

            @NotBlank @Size(min = 3, max = 100)
            String tenantSlug,

            @NotBlank @Size(min = 3, max = 255)
            String fullName,

            @NotBlank @Email
            String email,

            @NotBlank @Size(min = 8, max = 100)
            String password
    ) implements AuthRequest {}


    record Login(
            @NotBlank @Size(min = 3, max = 100)
            String tenantSlug,

            @NotBlank @Email
            String email,

            @NotBlank
            String password
    ) implements AuthRequest {}
}
