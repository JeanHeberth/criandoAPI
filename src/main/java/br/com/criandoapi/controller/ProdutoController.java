package br.com.criandoapi.controller;

import br.com.criandoapi.entity.ProdutoCategoria;
import br.com.criandoapi.record.EstoqueRequest;
import br.com.criandoapi.record.ProdutoRequest;
import br.com.criandoapi.record.ProdutoResponse;
import br.com.criandoapi.services.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    /**
     * Lista produtos com filtros opcionais e paginação.
     * Teste básico : GET /produtos
     * Teste paginação : GET /produtos?page=0&size=5&sort=preco,asc
     * Teste filtro nome: GET /produtos?nome=notebook
     * Teste filtro categoria: GET /produtos?categoria=ELETRONICO
     * Teste filtro preço: GET /produtos?precoMin=100&precoMax=500
     * [Público - sem token]
     */
    @GetMapping
    public ResponseEntity<Page<ProdutoResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) ProdutoCategoria categoria,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.listar(nome, categoria, precoMin, precoMax, pageable));
    }

    /**
     * Busca produto por ID.
     * Teste: id existente → 200 | id inexistente → 404
     * [Público - sem token]
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    /**
     * Lista produtos por categoria com paginação.
     * Teste: GET /produtos/categoria/ELETRONICO
     * [Público - sem token]
     */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<Page<ProdutoResponse>> listarPorCategoria(
            @PathVariable ProdutoCategoria categoria,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(produtoService.listarPorCategoria(categoria, pageable));
    }

    /**
     * Cria um novo produto.
     * Teste: campos válidos → 201 | nome duplicado → 409 | campos inválidos → 400
     * [Requer Bearer Token]
     */
    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(request));
    }

    /**
     * Atualiza produto completo.
     * Teste: id existente + dados válidos → 200 | id inexistente → 404
     * [Requer Bearer Token]
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.ok(produtoService.atualizar(id, request));
    }

    /**
     * Atualiza apenas o estoque do produto (PATCH parcial).
     * Teste básico: POST /produtos/{id}/estoque com {"quantidade": 50}
     * [Requer Bearer Token]
     */
    @PatchMapping("/{id}/estoque")
    public ResponseEntity<ProdutoResponse> atualizarEstoque(
            @PathVariable Long id, @Valid @RequestBody EstoqueRequest request) {
        return ResponseEntity.ok(produtoService.atualizarEstoque(id, request));
    }

    /**
     * Inativa produto (soft delete).
     * Teste: id existente → 204 | id inexistente → 404
     * [Requer Bearer Token]
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        produtoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

