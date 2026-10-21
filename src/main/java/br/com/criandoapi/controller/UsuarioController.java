package br.com.criandoapi.controller;

import br.com.criandoapi.record.UsuarioRequest;
import br.com.criandoapi.record.UsuarioResponse;
import br.com.criandoapi.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import br.com.criandoapi.security.UsuarioAutenticado;

@RestController
@RequestMapping("${api.version-prefix}/usuarios")
@Tag(name = "Usuarios", description = "CRUD de usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @Operation(
            summary = "Cria usuario",
            description = "Cria usuario sem autenticacao. CT base: sucesso (201), validacao (400), duplicidade de email (409)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "201 Created - Usuario criado"),
            @ApiResponse(responseCode = "400", description = "400 Bad Request - Dados invalidos"),
            @ApiResponse(responseCode = "409", description = "409 Conflict - Email ja cadastrado"),
            @ApiResponse(responseCode = "500", description = "500 Internal Server Error - Erro inesperado")
    })
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest usuarioRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criarUsuario(usuarioRequest));
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Lista usuarios",
            description = "Lista todos os usuarios; exige JWT de administrador. CT base: token valido (200), sem token (401)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Lista retornada"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "500", description = "500 Internal Server Error - Erro inesperado")
    })
    public ResponseEntity<List<UsuarioResponse>> listar(@RequestAttribute("usuarioAutenticado") UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(usuarioService.listarUsuarios(autenticado));
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Busca usuario por id",
            description = "Busca usuario por ID com JWT. CT base: encontrado (200), nao encontrado (404), sem token (401)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Usuario encontrado"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "404", description = "404 Not Found - Usuario nao encontrado"),
            @ApiResponse(responseCode = "500", description = "500 Internal Server Error - Erro inesperado")
    })
    public ResponseEntity<UsuarioResponse> buscarPorId(
            @Parameter(description = "ID do usuario", example = "1", required = true)
            @PathVariable Long id,
            @RequestAttribute("usuarioAutenticado") UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id, autenticado));
    }

    @GetMapping("/buscar")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Busca usuarios por nome",
            description = "Busca por nome; exige JWT de administrador. CT base: com resultado (200), sem resultado (404), sem token (401)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Busca executada"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "500", description = "500 Internal Server Error - Erro inesperado")
    })
    public ResponseEntity<List<UsuarioResponse>> buscarPorNome(
            @Parameter(description = "Nome ou parte do nome", example = "Joao", required = true)
            @RequestParam String nome,
            @RequestAttribute("usuarioAutenticado") UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(usuarioService.buscarPorNome(nome, autenticado));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Atualiza usuario",
            description = "Atualiza o proprio usuario ou permite administrador. CT base: sucesso (200), validacao (400), nao encontrado (404), email duplicado (409)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Usuario atualizado"),
            @ApiResponse(responseCode = "400", description = "400 Bad Request - Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "404", description = "404 Not Found - Usuario nao encontrado"),
            @ApiResponse(responseCode = "409", description = "409 Conflict - Email ja cadastrado"),
            @ApiResponse(responseCode = "500", description = "500 Internal Server Error - Erro inesperado")
    })
    public ResponseEntity<UsuarioResponse> atualizar(
            @Parameter(description = "ID do usuario", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequest usuarioRequest,
            @RequestAttribute("usuarioAutenticado") UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(usuarioService.atualizarUsuario(id, usuarioRequest, autenticado));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Remove usuario",
            description = "Remove o proprio usuario ou permite administrador. CT base: sucesso (204), nao encontrado (404), sem token (401)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "204 No Content - Usuario removido"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "404", description = "404 Not Found - Usuario nao encontrado"),
            @ApiResponse(responseCode = "500", description = "500 Internal Server Error - Erro inesperado")
    })
    public ResponseEntity<Void> deletar(
            @Parameter(description = "ID do usuario", example = "1", required = true)
            @PathVariable Long id,
            @RequestAttribute("usuarioAutenticado") UsuarioAutenticado autenticado) {
        usuarioService.deletarUsuario(id, autenticado);
        return ResponseEntity.noContent().build();
    }
}
