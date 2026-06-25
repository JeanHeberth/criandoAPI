package br.com.criandoapi.controller;

import br.com.criandoapi.record.AuthResponse;
import br.com.criandoapi.record.LoginRequest;
import br.com.criandoapi.record.RegistroRequest;
import br.com.criandoapi.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints de login e registro com JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Autentica um usuário",
            description = "Valida credenciais e retorna token JWT. CT base: credencial valida (200), invalida (401), campo invalido (400)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Login realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "400 Bad Request - Campos invalidos"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Credenciais invalidas")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    @Operation(
            summary = "Registra um novo usuário",
            description = "Cria conta e retorna token JWT. CT base: sucesso (201), validacao (400), email duplicado (409)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "201 Created - Usuario registrado"),
            @ApiResponse(responseCode = "400", description = "400 Bad Request - Dados invalidos"),
            @ApiResponse(responseCode = "409", description = "409 Conflict - Email ja cadastrado")
    })
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }
}
