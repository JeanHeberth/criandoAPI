package br.com.criandoapi.controller;

import br.com.criandoapi.entity.ProdutoCategoria;
import br.com.criandoapi.record.EstoqueRequest;
import br.com.criandoapi.record.ProdutoRequest;
import br.com.criandoapi.record.ProdutoResponse;
import br.com.criandoapi.services.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Produtos", description = "CRUD de produtos com paginação e filtros. GET é público, POST/PUT/PATCH/DELETE requer JWT")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    @Operation(
            summary = "Lista produtos com filtros e paginação",
            description = "Endpoint público (sem autenticação). Suporta filtros por nome, categoria, faixa de preço e paginação com sorting customizável."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de produtos (paginada)",
            content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Page<ProdutoResponse>> listar(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String nome,
            @org.springframework.web.bind.annotation.RequestParam(required = false) ProdutoCategoria categoria,
            @org.springframework.web.bind.annotation.RequestParam(required = false) BigDecimal precoMin,
            @org.springframework.web.bind.annotation.RequestParam(required = false) BigDecimal precoMax,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.listar(nome, categoria, precoMin, precoMax, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca produto por ID",
            description = "Endpoint público. Retorna detalhes de um produto específico."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<ProdutoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @GetMapping("/categoria/{categoria}")
    @Operation(
            summary = "Lista produtos por categoria",
            description = "Endpoint público. Retorna produtos de uma categoria específica com paginação."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Produtos encontrados (paginados)"
    )
    public ResponseEntity<Page<ProdutoResponse>> listarPorCategoria(
            @PathVariable ProdutoCategoria categoria,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(produtoService.listarPorCategoria(categoria, pageable));
    }

    @PostMapping
    @Operation(
            summary = "Cria novo produto",
            description = "Requer autenticação com token JWT. Cria um novo produto no catálogo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Produto criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (preço negativo, nome vazio, etc)"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "409", description = "Conflito: produto com nome idêntico já existe")
    })
    public ResponseEntity<ProdutoResponse> criar(@Valid @org.springframework.web.bind.annotation.RequestBody ProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualiza um produto",
            description = "Requer autenticação com token JWT. Atualiza todas as informações do produto."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "409", description = "Nome do produto já existe")
    })
    public ResponseEntity<ProdutoResponse> atualizar(
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody ProdutoRequest request) {
        return ResponseEntity.ok(produtoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/estoque")
    @Operation(
            summary = "Atualiza estoque do produto (PATCH)",
            description = "Requer autenticação com token JWT. Atualiza apenas a quantidade em estoque sem alterar outros dados."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estoque atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<ProdutoResponse> atualizarEstoque(
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody EstoqueRequest request) {
        return ResponseEntity.ok(produtoService.atualizarEstoque(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Inativa um produto (soft delete)",
            description = "Requer autenticação com token JWT. Marca o produto como inativo sem deletar os dados (soft delete)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Produto inativado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        produtoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

