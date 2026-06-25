package br.com.criandoapi.controller;

import br.com.criandoapi.entity.ProdutoCategoria;
import br.com.criandoapi.record.EstoqueRequest;
import br.com.criandoapi.record.ProdutoRequest;
import br.com.criandoapi.record.ProdutoResponse;
import br.com.criandoapi.services.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/produtos")
@Tag(name = "Produtos", description = "CRUD de produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    @Operation(
            summary = "Lista produtos",
            description = "Endpoint publico com filtros e paginacao. Use sort no formato campo,direcao (ex.: nome,asc). CT base: sem filtro (200), com filtro (200), parametro invalido (400)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Lista paginada"),
            @ApiResponse(responseCode = "400", description = "400 Bad Request - Parametro invalido")
    })
    public ResponseEntity<Page<ProdutoResponse>> listar(
            @Parameter(description = "Filtro por nome", example = "Notebook")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Filtro por categoria", example = "ELETRONICO")
            @RequestParam(required = false) ProdutoCategoria categoria,
            @Parameter(description = "Preco minimo", example = "100.00")
            @RequestParam(required = false) BigDecimal precoMin,
            @Parameter(description = "Preco maximo", example = "5000.00")
            @RequestParam(required = false) BigDecimal precoMax,
            @Parameter(description = "Numero da pagina (inicia em 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de itens por pagina", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Ordenacao no formato campo,direcao. Direcao aceita: asc ou desc.", example = "nome,asc")
            @RequestParam(defaultValue = "nome,asc") String sort) {
        Pageable pageable = buildPageable(page, size, sort, "nome", Sort.Direction.ASC);
        return ResponseEntity.ok(produtoService.listar(nome, categoria, precoMin, precoMax, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca produto por id",
            description = "Endpoint publico. CT base: encontrado (200), nao encontrado (404)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "404 Not Found - Produto nao encontrado")
    })
    public ResponseEntity<ProdutoResponse> buscarPorId(
            @Parameter(description = "ID do produto", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @GetMapping("/categoria/{categoria}")
    @Operation(
            summary = "Lista por categoria",
            description = "Endpoint publico por categoria com paginacao. Use sort no formato campo,direcao (ex.: nome,asc). CT base: categoria valida (200), invalida (400)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Lista paginada"),
            @ApiResponse(responseCode = "400", description = "400 Bad Request - Categoria invalida")
    })
    public ResponseEntity<Page<ProdutoResponse>> listarPorCategoria(
            @Parameter(description = "Categoria do produto", example = "ELETRONICO", required = true)
            @PathVariable ProdutoCategoria categoria,
            @Parameter(description = "Numero da pagina (inicia em 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de itens por pagina", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Ordenacao no formato campo,direcao. Direcao aceita: asc ou desc.", example = "nome,asc")
            @RequestParam(defaultValue = "nome,asc") String sort) {
        Pageable pageable = buildPageable(page, size, sort, "nome", Sort.Direction.ASC);
        return ResponseEntity.ok(produtoService.listarPorCategoria(categoria, pageable));
    }

    private Pageable buildPageable(int page, int size, String sortParam, String defaultField, Sort.Direction defaultDirection) {
        String normalized = (sortParam == null || sortParam.isBlank())
                ? defaultField + "," + defaultDirection.name().toLowerCase()
                : sortParam;
        String[] parts = normalized.split(",", 2);
        String field = parts[0].isBlank() ? defaultField : parts[0].trim();
        Sort.Direction direction;
        if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
            direction = Sort.Direction.DESC;
        } else if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
            direction = Sort.Direction.ASC;
        } else {
            direction = defaultDirection;
        }

        return PageRequest.of(page, size, Sort.by(direction, field));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Cria produto",
            description = "Cria produto com JWT. CT base: sucesso (201), validacao (400), sem token (401), duplicidade (409)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "201 Created - Produto criado"),
            @ApiResponse(responseCode = "400", description = "400 Bad Request - Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "409", description = "409 Conflict - Nome ja cadastrado")
    })
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(request));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Atualiza produto",
            description = "Atualiza produto com JWT. CT base: sucesso (200), validacao (400), sem token (401), nao encontrado (404), duplicidade (409)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Produto atualizado"),
            @ApiResponse(responseCode = "400", description = "400 Bad Request - Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "404", description = "404 Not Found - Produto nao encontrado"),
            @ApiResponse(responseCode = "409", description = "409 Conflict - Nome ja cadastrado")
    })
    public ResponseEntity<ProdutoResponse> atualizar(
            @Parameter(description = "ID do produto", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.ok(produtoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/estoque")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Atualiza estoque",
            description = "Atualiza apenas estoque com JWT. CT base: sucesso (200), validacao (400), sem token (401), nao encontrado (404)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Estoque atualizado"),
            @ApiResponse(responseCode = "400", description = "400 Bad Request - Quantidade invalida"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "404", description = "404 Not Found - Produto nao encontrado")
    })
    public ResponseEntity<ProdutoResponse> atualizarEstoque(
            @Parameter(description = "ID do produto", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody EstoqueRequest request) {
        return ResponseEntity.ok(produtoService.atualizarEstoque(id, request));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Inativa produto",
            description = "Soft delete com JWT. CT base: sucesso (204), sem token (401), nao encontrado (404)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "204 No Content - Produto inativado"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "404", description = "404 Not Found - Produto nao encontrado")
    })
    public ResponseEntity<Void> deletar(
            @Parameter(description = "ID do produto", example = "1", required = true)
            @PathVariable Long id) {
        produtoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
