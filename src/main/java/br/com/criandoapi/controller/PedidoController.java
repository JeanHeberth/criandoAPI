package br.com.criandoapi.controller;

import br.com.criandoapi.entity.PedidoStatus;
import br.com.criandoapi.record.PedidoRequest;
import br.com.criandoapi.record.PedidoResponse;
import br.com.criandoapi.record.StatusRequest;
import br.com.criandoapi.services.PedidoService;
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
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /**
     * Cria um novo pedido para o usuário autenticado.
     * Teste: produto sem estoque → 422 | produto inativo → 422 | sem itens → 400
     * Teste avançado: verifica se estoque foi baixado após criação
     * [Requer Bearer Token]
     */
    @PostMapping
    public ResponseEntity<PedidoResponse> criar(
            @Valid @RequestBody PedidoRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criar(request, httpRequest));
    }

    /**
     * Lista pedidos do usuário autenticado.
     * Teste paginação: GET /pedidos?page=0&size=5
     * Teste filtro status: GET /pedidos?status=PENDENTE
     * [Requer Bearer Token]
     */
    @GetMapping
    public ResponseEntity<Page<PedidoResponse>> listar(
            @RequestParam(required = false) PedidoStatus status,
            @PageableDefault(size = 10, sort = "criadoEm") Pageable pageable,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(pedidoService.listar(status, httpRequest, pageable));
    }

    /**
     * Busca pedido específico do usuário autenticado.
     * Teste: pedido de outro usuário → 404 (isolamento por usuário)
     * [Requer Bearer Token]
     */
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id, httpRequest));
    }

    /**
     * Transiciona o status do pedido.
     * Fluxo: PENDENTE → CONFIRMADO → EM_PREPARO → ENVIADO → ENTREGUE
     * Cancelamento: PENDENTE ou CONFIRMADO → CANCELADO
     * Teste avançado: transição inválida → 422 com mensagem explicativa
     * [Requer Bearer Token]
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<PedidoResponse> transicionarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(pedidoService.transicionarStatus(id, request, httpRequest));
    }

    /**
     * Cancela pedido (somente se PENDENTE). Devolve estoque automaticamente.
     * Teste: cancelar pedido CONFIRMADO → 422 | cancelar pedido PENDENTE → 204
     * [Requer Bearer Token]
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, HttpServletRequest httpRequest) {
        pedidoService.cancelar(id, httpRequest);
        return ResponseEntity.noContent().build();
    }
}

