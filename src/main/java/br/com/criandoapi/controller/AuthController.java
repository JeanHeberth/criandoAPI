package br.com.criandoapi.controller;

import br.com.criandoapi.record.AuthResponse;
import br.com.criandoapi.record.LoginRequest;
import br.com.criandoapi.record.RegistroRequest;
import br.com.criandoapi.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
            summary = "Autentica um usuário existente",
            description = "Verifica credenciais e retorna token JWT válido por 24 horas. Sem necessidade de autenticação prévia."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login bem-sucedido",
                    content = @Content(mediaType = "application/json", schema = @Schema(
                            example = "{\"token\":\"eyJhbGciOiJIUzUxMiJ9...\",\"tipo\":\"Bearer\",\"usuario\":{\"id\":1,\"nome\":\"João\",\"email\":\"joao@test.com\"}}"
                    ))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Campos inválidos (e-mail ou senha em branco)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciais inválidas (usuário não existe ou senha errada)"
            )
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    @Operation(
            summary = "Registra um novo usuário",
            description = "Cria uma nova conta e retorna token JWT pronto para uso. Sem necessidade de autenticação prévia."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário registrado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(
                            example = "{\"token\":\"eyJhbGciOiJIUzUxMiJ9...\",\"tipo\":\"Bearer\",\"usuario\":{\"id\":1,\"nome\":\"João\",\"email\":\"joao@test.com\"}}"
                    ))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Campos inválidos (nome curto, email mal formatado, senha fraca)"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito: e-mail já cadastrado no sistema"
            )
    })
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }
}

