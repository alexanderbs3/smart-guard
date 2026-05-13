package br.leetjourney.smartguard.interfaces.rest;


import br.leetjourney.smartguard.application.dto.AuthRequest;
import br.leetjourney.smartguard.application.dto.AuthResponse;
import br.leetjourney.smartguard.domain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro de tenant e autenticação de usuários")
public class AuthController {


    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registrar novo tenant",
            description = "Cria um tenant e um usuário TENANT_ADMIN. Retorna o JWT de acesso."
    )
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest.Register request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Autentica um usuário dentro de um tenant. Retorna o JWT de acesso."
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest.Login request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
