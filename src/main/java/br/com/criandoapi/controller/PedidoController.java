package br.com.criandoapi.controller;

import br.com.criandoapi.entity.PedidoStatus;
import br.com.criandoapi.record.PedidoRequest;
import br.com.criandoapi.record.PedidoResponse;
import br.com.criandoapi.record.StatusRequest;
import br.com.criandoapi.services.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.version-prefix}/pedidos")
@Tag(name = "Pedidos", description = "CRUD de pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Cria pedido",
            description = "Cria pedido com JWT e baixa estoque. CT base: sucesso (201), validacao (400), sem token (401), produto nao encontrado (404), regra de negocio (422)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "201 Created - Pedido criado"),
            @ApiResponse(responseCode = "400", description = "400 Bad Request - Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "404", description = "404 Not Found - Produto nao encontrado"),
            @ApiResponse(responseCode = "422", description = "422 Unprocessable Entity - Regra de negocio violada")
    })
    public ResponseEntity<PedidoResponse> criar(
            @Valid @RequestBody PedidoRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criar(request, httpRequest));
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Lista pedidos do usuario",
            description = "Lista pedidos do usuario autenticado com paginacao. Use sort no formato campo,direcao (ex.: criadoEm,desc). CT base: sucesso (200), sem token (401)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Lista paginada"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido")
    })
    public ResponseEntity<Page<PedidoResponse>> listar(
            @Parameter(description = "Filtro por status", example = "PENDENTE")
            @RequestParam(required = false) PedidoStatus status,
            @Parameter(description = "Numero da pagina (inicia em 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de itens por pagina", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Ordenacao no formato campo,direcao. Direcao aceita: asc ou desc.", example = "criadoEm,desc")
            @RequestParam(defaultValue = "criadoEm,desc") String sort,
            HttpServletRequest httpRequest) {
        Pageable pageable = buildPageable(page, size, sort, "criadoEm");
        return ResponseEntity.ok(pedidoService.listar(status, httpRequest, pageable));
    }

    private Pageable buildPageable(int page, int size, String sortParam, String defaultField) {
        String normalized = (sortParam == null || sortParam.isBlank()) ? defaultField + ",desc" : sortParam;
        String[] parts = normalized.split(",", 2);
        String field = parts[0].isBlank() ? defaultField : parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, field));
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Busca pedido por id",
            description = "Busca pedido por ID com isolamento por usuario. CT base: encontrado (200), sem token (401), nao encontrado/sem permissao (404)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Pedido encontrado"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "404", description = "404 Not Found - Pedido nao encontrado")
    })
    public ResponseEntity<PedidoResponse> buscarPorId(
            @Parameter(description = "ID do pedido", example = "1", required = true)
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id, httpRequest));
    }

    @PatchMapping("/{id}/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Transiciona status do pedido",
            description = "Altera status conforme maquina de estados. CT base: transicao valida (200), dados invalidos (400), sem token (401), nao encontrado (404), transicao invalida (422)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "200 OK - Status atualizado"),
            @ApiResponse(responseCode = "400", description = "400 Bad Request - Status invalido/ausente"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "404", description = "404 Not Found - Pedido nao encontrado"),
            @ApiResponse(responseCode = "422", description = "422 Unprocessable Entity - Transicao invalida")
    })
    public ResponseEntity<PedidoResponse> transicionarStatus(
            @Parameter(description = "ID do pedido", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(pedidoService.transicionarStatus(id, request, httpRequest));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Cancela pedido",
            description = "Cancela pedido PENDENTE e devolve estoque. CT base: sucesso (204), sem token (401), nao encontrado (404), regra de negocio (422)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "204 No Content - Pedido cancelado"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido"),
            @ApiResponse(responseCode = "404", description = "404 Not Found - Pedido nao encontrado"),
            @ApiResponse(responseCode = "422", description = "422 Unprocessable Entity - Status nao permite cancelamento")
    })
    public ResponseEntity<Void> cancelar(
            @Parameter(description = "ID do pedido", example = "1", required = true)
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        pedidoService.cancelar(id, httpRequest);
        return ResponseEntity.noContent().build();
    }
}
