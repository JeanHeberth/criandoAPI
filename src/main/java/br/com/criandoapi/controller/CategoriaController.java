package br.com.criandoapi.controller;

import br.com.criandoapi.entity.UsuarioRole;
import br.com.criandoapi.record.CategoriaRequest;
import br.com.criandoapi.record.CategoriaResponse;
import br.com.criandoapi.record.CategoriaUpdateRequest;
import br.com.criandoapi.record.PageResponse;
import br.com.criandoapi.security.RequiresRole;
import br.com.criandoapi.services.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    @RequiresRole(UsuarioRole.ADMIN)
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.criar(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CategoriaResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String nome
    ) {
        if (nome != null && !nome.isBlank()) {
            return ResponseEntity.ok(categoriaService.buscarPorNome(nome, page, size));
        }
        return ResponseEntity.ok(categoriaService.listar(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @RequiresRole(UsuarioRole.ADMIN)
    public ResponseEntity<CategoriaResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaUpdateRequest request
    ) {
        return ResponseEntity.ok(categoriaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @RequiresRole(UsuarioRole.ADMIN)
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        categoriaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
