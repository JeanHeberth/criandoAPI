package br.com.criandoapi.controller;

import br.com.criandoapi.entity.UsuarioRole;
import br.com.criandoapi.record.PageResponse;
import br.com.criandoapi.record.ProdutoRequest;
import br.com.criandoapi.record.ProdutoResponse;
import br.com.criandoapi.record.ProdutoUpdateRequest;
import br.com.criandoapi.security.RequiresRole;
import br.com.criandoapi.services.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    @RequiresRole(UsuarioRole.ADMIN)
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProdutoResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Long categoriaId
    ) {
        if (nome != null && !nome.isBlank()) {
            return ResponseEntity.ok(produtoService.buscarPorNome(nome, page, size));
        }
        if (categoriaId != null) {
            return ResponseEntity.ok(produtoService.buscarPorCategoria(categoriaId, page, size));
        }
        return ResponseEntity.ok(produtoService.listar(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @RequiresRole(UsuarioRole.ADMIN)
    public ResponseEntity<ProdutoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoUpdateRequest request
    ) {
        return ResponseEntity.ok(produtoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @RequiresRole(UsuarioRole.ADMIN)
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        produtoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
