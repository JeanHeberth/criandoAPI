package br.com.criandoapi.controller;

import br.com.criandoapi.entity.UsuarioRole;
import br.com.criandoapi.record.PageResponse;
import br.com.criandoapi.record.UsuarioAdminRequest;
import br.com.criandoapi.record.UsuarioResponse;
import br.com.criandoapi.record.UsuarioUpdateRequest;
import br.com.criandoapi.security.RequiresRole;
import br.com.criandoapi.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @RequiresRole(UsuarioRole.ADMIN)
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criarUsuarioAdmin(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<UsuarioResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String nome
    ) {
        if (nome != null && !nome.isBlank()) {
            return ResponseEntity.ok(usuarioService.buscarPorNome(nome, page, size));
        }
        return ResponseEntity.ok(usuarioService.listar(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @RequiresRole(UsuarioRole.ADMIN)
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequest usuarioRequest
    ) {
        return ResponseEntity.ok(usuarioService.atualizarUsuario(id, usuarioRequest));
    }

    @DeleteMapping("/{id}")
    @RequiresRole(UsuarioRole.ADMIN)
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
