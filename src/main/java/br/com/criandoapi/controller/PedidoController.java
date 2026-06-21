package br.com.criandoapi.controller;

import br.com.criandoapi.entity.PedidoStatus;
import br.com.criandoapi.record.PedidoRequest;
import br.com.criandoapi.record.PedidoResponse;
import br.com.criandoapi.record.StatusRequest;
import br.com.criandoapi.services.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos", description = "CRUD de pedidos com máquina de estados. Requer autenticação JWT")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @Operation(
            summary = "Cria um novo pedido",
            description = "Requer autenticação com token JWT. Cria um pedido para o usuário autenticado e baixa automaticamente o estoque dos produtos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (lista de itens vazia, quantidade negativa, etc)"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Erro de negócio: estoque insuficiente ou produto inativo",
                    content = @Content(mediaType = "application/json", schema = @Schema(
                            example = "{\"status\":422,\"erro\":\"Erro de Negócio\",\"mensagem\":\"Estoque insuficiente para o produto 'Notebook'. Disponível: 2, Solicitado: 5\"}"
                    ))
            )
    })
    public ResponseEntity<PedidoResponse> criar(
            @Valid @org.springframework.web.bind.annotation.RequestBody PedidoRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criar(request, httpRequest));
    }

    @GetMapping
    @Operation(
            summary = "Lista pedidos do usuário autenticado",
            description = "Requer autenticação com token JWT. Retorna pedidos do usuário logado com paginação e filtro opcional por status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos (paginada)"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<Page<PedidoResponse>> listar(
            @org.springframework.web.bind.annotation.RequestParam(required = false) PedidoStatus status,
            @PageableDefault(size = 10, sort = "criadoEm") Pageable pageable,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(pedidoService.listar(status, httpRequest, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca pedido por ID",
            description = "Requer autenticação com token JWT. Retorna detalhes de um pedido específico. Isolamento por usuário: só pode acessar pedidos próprios."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Pedido não encontrado ou pertence a outro usuário (isolamento)"
            )
    })
    public ResponseEntity<PedidoResponse> buscarPorId(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id, httpRequest));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Transiciona o status do pedido (máquina de estados)",
            description = "Requer autenticação com token JWT. Transiciona o pedido entre estados: PENDENTE → CONFIRMADO → EM_PREPARO → ENVIADO → ENTREGUE. Cancelamento permitido de PENDENTE e CONFIRMADO."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Status transicionado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(
                            example = "{\"id\":1,\"status\":\"CONFIRMADO\",\"valorTotal\":7000.00}"
                    ))
            ),
            @ApiResponse(responseCode = "400", description = "Status inválido no request"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Transição de estado inválida",
                    content = @Content(mediaType = "application/json", schema = @Schema(
                            example = "{\"status\":422,\"erro\":\"Erro de Negócio\",\"mensagem\":\"Transição inválida: ENTREGUE → PENDENTE. Transições permitidas a partir de ENTREGUE: []\"}"
                    ))
            )
    })
    public ResponseEntity<PedidoResponse> transicionarStatus(
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody StatusRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(pedidoService.transicionarStatus(id, request, httpRequest));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancela um pedido",
            description = "Requer autenticação com token JWT. Cancela o pedido e devolve o estoque dos produtos (somente se status = PENDENTE)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pedido cancelado com sucesso e estoque devolvido"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Erro de negócio: pedido não está em status PENDENTE",
                    content = @Content(mediaType = "application/json", schema = @Schema(
                            example = "{\"status\":422,\"erro\":\"Erro de Negócio\",\"mensagem\":\"Apenas pedidos com status PENDENTE podem ser cancelados. Status atual: CONFIRMADO\"}"
                    ))
            )
    })
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        pedidoService.cancelar(id, httpRequest);
        return ResponseEntity.noContent().build();
    }
}

