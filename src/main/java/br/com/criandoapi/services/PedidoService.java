package br.com.criandoapi.services;

import br.com.criandoapi.entity.*;
import br.com.criandoapi.exception.NegocioException;
import br.com.criandoapi.record.*;
import br.com.criandoapi.repository.PedidoRepository;
import br.com.criandoapi.repository.ProdutoRepository;
import br.com.criandoapi.repository.UsuarioRepository;
import br.com.criandoapi.security.UsuarioAutenticado;
import br.com.criandoapi.exception.ConflictException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
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
    public PedidoResponse criar(PedidoRequest request, UsuarioAutenticado autenticado) {
        return criar(request, autenticado, null);
    }

    @Transactional
    public PedidoResponse criar(PedidoRequest request, UsuarioAutenticado autenticado, String chave) {
        if (chave != null && !chave.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new ResponseStatusException(BAD_REQUEST, "Idempotency-Key deve ter 1 a 128 caracteres: letras, numeros, ponto, sublinhado, dois-pontos ou hifen");
        }
        Usuario usuario = usuarioRepository.findByIdForUpdate(autenticado.id())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario nao encontrado"));
        String hash = hashRequest(request);
        String hashChave = chave == null ? null : hash(chave);
        if (chave != null) {
            Pedido existente = pedidoRepository.findByUsuarioIdAndIdempotencyKeyHash(usuario.getId(), hashChave).orElse(null);
            if (existente != null) {
                if (!hash.equals(existente.getRequestHash())) {
                    throw new ConflictException("Idempotency-Key ja utilizada com outro payload");
                }
                return toResponse(existente);
            }
        }
        Pedido pedido = new Pedido(usuario);
        pedido.setIdempotencyKey(chave);
        pedido.setIdempotencyKeyHash(hashChave);
        pedido.setRequestHash(chave == null ? null : hash);
        // Acquire locks in product ID order to avoid opposing lock order between orders.
        Map<Long, Produto> produtos = new TreeMap<>();
        request.itens().stream().map(ItemPedidoRequest::produtoId).distinct().sorted().forEach(id ->
                produtos.put(id, produtoRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Produto nao encontrado com id: " + id))));
        for (ItemPedidoRequest itemReq : request.itens()) {
            Produto produto = produtos.get(itemReq.produtoId());
            if (!produto.getAtivo()) {
                throw new NegocioException("Produto '" + produto.getNome() + "' esta inativo e nao pode ser pedido");
            }
            if (produto.getEstoque() < itemReq.quantidade()) {
                throw new NegocioException("Estoque insuficiente para o produto '" + produto.getNome() + "'");
            }
            pedido.getItens().add(new ItemPedido(pedido, produto, itemReq.quantidade()));
            produto.atualizarEstoque(produto.getEstoque() - itemReq.quantidade());
        }
        pedido.calcularTotal();
        validarLimiteValorTotal(pedido.getValorTotal());
        produtos.values().forEach(produtoRepository::save);
        return toResponse(pedidoRepository.save(pedido));
    }

    private String hashRequest(PedidoRequest request) {
        return hash(request.itens().stream().map(item -> item.produtoId() + ":" + item.quantidade())
                .collect(java.util.stream.Collectors.joining(";")));
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void validarLimiteValorTotal(BigDecimal valorTotal) {
        if (valorTotal.compareTo(LIMITE_MAXIMO_VALOR_TOTAL) > 0) {
            throw new NegocioException(
                    "Valor total do pedido excede o limite permitido de 9.999.999.999.999,99. Revise quantidade ou valor dos itens."
            );
        }
    }

    public Page<PedidoResponse> listar(PedidoStatus status, UsuarioAutenticado autenticado, Pageable pageable) {
        Long usuarioId = autenticado.id();
        if (status != null) {
            return pedidoRepository.findByUsuarioIdAndStatus(usuarioId, status, pageable).map(this::toResponse);
        }
        return pedidoRepository.findByUsuarioId(usuarioId, pageable).map(this::toResponse);
    }

    public PedidoResponse buscarPorId(Long id, UsuarioAutenticado autenticado) {
        Long usuarioId = autenticado.id();
        Pedido pedido = pedidoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido não encontrado com id: " + id));
        return toResponse(pedido);
    }

    @Transactional
    public PedidoResponse transicionarStatus(Long id, StatusRequest request, UsuarioAutenticado autenticado) {
        Long usuarioId = autenticado.id();
        Pedido pedido = pedidoRepository.findByIdAndUsuarioIdForUpdate(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido não encontrado com id: " + id));

        PedidoStatus novoStatus = request.status();

        if (!pedido.getStatus().podeTransicionarPara(novoStatus)) {
            throw new NegocioException(
                    "Transição inválida: " + pedido.getStatus() + " → " + novoStatus +
                    ". Transições permitidas a partir de " + pedido.getStatus() + ": " +
                    transicoesPossiveis(pedido.getStatus())
            );
        }

        if (novoStatus == PedidoStatus.CANCELADO) {
            devolverEstoque(pedido);
        }
        pedido.transicionarStatus(novoStatus);
        return toResponse(pedidoRepository.save(pedido));
    }

    @Transactional
    public void cancelar(Long id, UsuarioAutenticado autenticado) {
        Long usuarioId = autenticado.id();
        Pedido pedido = pedidoRepository.findByIdAndUsuarioIdForUpdate(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido não encontrado com id: " + id));

        if (!pedido.getStatus().podeTransicionarPara(PedidoStatus.CANCELADO)) {
            throw new NegocioException("Apenas pedidos PENDENTE ou CONFIRMADO podem ser cancelados");
        }
        devolverEstoque(pedido);
        pedido.transicionarStatus(PedidoStatus.CANCELADO);
        pedidoRepository.save(pedido);
    }

    private void devolverEstoque(Pedido pedido) {
        Map<Long, Long> quantidades = new TreeMap<>();
        pedido.getItens().forEach(item -> quantidades.merge(item.getProduto().getId(),
                item.getQuantidade().longValue(), Long::sum));
        quantidades.forEach((id, quantidade) -> {
            Produto produto = produtoRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Produto nao encontrado"));
            long estoque = produto.getEstoque().longValue() + quantidade;
            if (estoque > Integer.MAX_VALUE) {
                throw new NegocioException("Devolucao excede o limite de estoque do produto");
            }
            produto.atualizarEstoque((int) estoque);
            produtoRepository.save(produto);
        });
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

