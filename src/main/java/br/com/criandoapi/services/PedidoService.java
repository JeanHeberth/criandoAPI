package br.com.criandoapi.services;

import br.com.criandoapi.entity.*;
import br.com.criandoapi.exception.NegocioException;
import br.com.criandoapi.record.*;
import br.com.criandoapi.repository.PedidoRepository;
import br.com.criandoapi.repository.ProdutoRepository;
import br.com.criandoapi.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PedidoService {

    private static final BigDecimal LIMITE_MAXIMO_VALOR_TOTAL = new BigDecimal("9999999999999.99");

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProdutoRepository produtoRepository,
                         UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public PedidoResponse criar(PedidoRequest request, HttpServletRequest httpRequest) {
        String email = (String) httpRequest.getAttribute("usuarioEmail");
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuário não encontrado"));

        Pedido pedido = new Pedido(usuario);

        for (ItemPedidoRequest itemReq : request.itens()) {
            Produto produto = produtoRepository.findById(itemReq.produtoId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                            "Produto não encontrado com id: " + itemReq.produtoId()));

            if (!produto.getAtivo()) {
                throw new NegocioException("Produto '" + produto.getNome() + "' está inativo e não pode ser pedido");
            }

            if (produto.getEstoque() < itemReq.quantidade()) {
                throw new NegocioException("Estoque insuficiente para o produto '" + produto.getNome() +
                        "'. Disponível: " + produto.getEstoque() + ", Solicitado: " + itemReq.quantidade());
            }

            ItemPedido item = new ItemPedido(pedido, produto, itemReq.quantidade());
            pedido.getItens().add(item);

            // Baixa estoque
            produto.atualizarEstoque(produto.getEstoque() - itemReq.quantidade());
            produtoRepository.save(produto);
        }

        pedido.calcularTotal();
        validarLimiteValorTotal(pedido.getValorTotal());
        return toResponse(pedidoRepository.save(pedido));
    }

    private void validarLimiteValorTotal(BigDecimal valorTotal) {
        if (valorTotal.compareTo(LIMITE_MAXIMO_VALOR_TOTAL) > 0) {
            throw new NegocioException(
                    "Valor total do pedido excede o limite permitido de 9.999.999.999.999,99. Revise quantidade ou valor dos itens."
            );
        }
    }

    public Page<PedidoResponse> listar(PedidoStatus status, HttpServletRequest httpRequest, Pageable pageable) {
        Long usuarioId = getUsuarioId(httpRequest);
        if (status != null) {
            return pedidoRepository.findByUsuarioIdAndStatus(usuarioId, status, pageable).map(this::toResponse);
        }
        return pedidoRepository.findByUsuarioId(usuarioId, pageable).map(this::toResponse);
    }

    public PedidoResponse buscarPorId(Long id, HttpServletRequest httpRequest) {
        Long usuarioId = getUsuarioId(httpRequest);
        Pedido pedido = pedidoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido não encontrado com id: " + id));
        return toResponse(pedido);
    }

    @Transactional
    public PedidoResponse transicionarStatus(Long id, StatusRequest request, HttpServletRequest httpRequest) {
        Long usuarioId = getUsuarioId(httpRequest);
        Pedido pedido = pedidoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido não encontrado com id: " + id));

        PedidoStatus novoStatus = request.status();

        if (!pedido.getStatus().podeTransicionarPara(novoStatus)) {
            throw new NegocioException(
                    "Transição inválida: " + pedido.getStatus() + " → " + novoStatus +
                    ". Transições permitidas a partir de " + pedido.getStatus() + ": " +
                    transicoesPossiveis(pedido.getStatus())
            );
        }

        pedido.transicionarStatus(novoStatus);
        return toResponse(pedidoRepository.save(pedido));
    }

    @Transactional
    public void cancelar(Long id, HttpServletRequest httpRequest) {
        Long usuarioId = getUsuarioId(httpRequest);
        Pedido pedido = pedidoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido não encontrado com id: " + id));

        if (pedido.getStatus() != PedidoStatus.PENDENTE) {
            throw new NegocioException(
                    "Apenas pedidos com status PENDENTE podem ser cancelados. Status atual: " + pedido.getStatus()
            );
        }

        // Devolve estoque
        pedido.getItens().forEach(item -> {
            Produto produto = item.getProduto();
            produto.atualizarEstoque(produto.getEstoque() + item.getQuantidade());
            produtoRepository.save(produto);
        });

        pedido.transicionarStatus(PedidoStatus.CANCELADO);
        pedidoRepository.save(pedido);
    }

    private Long getUsuarioId(HttpServletRequest httpRequest) {
        String email = (String) httpRequest.getAttribute("usuarioEmail");
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Acesso negado"))
                .getId();
    }

    private String transicoesPossiveis(PedidoStatus status) {
        return switch (status) {
            case PENDENTE    -> "[CONFIRMADO, CANCELADO]";
            case CONFIRMADO  -> "[EM_PREPARO, CANCELADO]";
            case EM_PREPARO  -> "[ENVIADO]";
            case ENVIADO     -> "[ENTREGUE]";
            default          -> "[]";
        };
    }

    private PedidoResponse toResponse(Pedido pedido) {
        List<ItemPedidoResponse> itens = pedido.getItens().stream()
                .map(i -> new ItemPedidoResponse(
                        i.getId(),
                        i.getProduto().getId(),
                        i.getProduto().getNome(),
                        i.getQuantidade(),
                        i.getPrecoUnitario(),
                        i.getSubtotal()
                ))
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getUsuario().getId(),
                pedido.getUsuario().getNome(),
                itens,
                pedido.getStatus(),
                pedido.getValorTotal(),
                pedido.getCriadoEm(),
                pedido.getAtualizadoEm()
        );
    }
}

