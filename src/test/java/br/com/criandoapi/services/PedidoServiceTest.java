package br.com.criandoapi.services;

import br.com.criandoapi.entity.Pedido;
import br.com.criandoapi.entity.PedidoStatus;
import br.com.criandoapi.entity.ItemPedido;
import br.com.criandoapi.exception.NegocioException;
import br.com.criandoapi.record.PedidoResponse;
import br.com.criandoapi.record.StatusRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.web.server.ResponseStatusException;
import java.util.stream.Stream;
import br.com.criandoapi.entity.Produto;
import br.com.criandoapi.entity.ProdutoCategoria;
import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.record.ItemPedidoRequest;
import br.com.criandoapi.record.PedidoRequest;
import br.com.criandoapi.repository.PedidoRepository;
import br.com.criandoapi.repository.ProdutoRepository;
import br.com.criandoapi.repository.UsuarioRepository;
import br.com.criandoapi.security.UsuarioAutenticado;
import br.com.criandoapi.entity.UsuarioRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private final UsuarioAutenticado autenticado = new UsuarioAutenticado(1L, "teste@api.com", UsuarioRole.USER);

    @InjectMocks
    private PedidoService pedidoService;

    @Test
    @DisplayName("deve permitir criar pedido com quantidade 45035 quando estoque suporta")
    void devePermitirCriarPedidoComQuantidadeAlta() {
        Usuario usuario = new Usuario("Usuario Teste", "teste@api.com", "123456");
        usuario.setId(1L);

        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Notebook");
        produto.setPreco(new BigDecimal("3000.00"));
        produto.setEstoque(50000);
        produto.setAtivo(true);
        produto.setCategoria(ProdutoCategoria.ELETRONICO);

        PedidoRequest request = new PedidoRequest(List.of(new ItemPedidoRequest(1L, 45035)));

        when(usuarioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(usuario));
        when(produtoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(produto));

        when(pedidoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoResponse response = pedidoService.criar(request, autenticado);

        assertEquals(4965, produto.getEstoque());
        assertEquals(PedidoStatus.PENDENTE, response.status());
        assertEquals(1L, response.usuarioId());
        assertEquals(1, response.itens().size());
        assertEquals(45035, response.itens().get(0).quantidade());
        assertEquals(new BigDecimal("135105000.00"), response.valorTotal());
        assertEquals(response.valorTotal(), response.itens().get(0).subtotal());
        verify(produtoRepository).save(produto);
        verify(pedidoRepository).save(any());
    }

    private Usuario autenticar() {
        Usuario usuario = new Usuario("Usuario Teste", "teste@api.com", "123456");
        usuario.setId(1L);
        return usuario;
    }

    private Produto produto(int estoque, boolean ativo) {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Notebook");
        produto.setPreco(new BigDecimal("3000.00"));
        produto.setEstoque(estoque);
        produto.setAtivo(ativo);
        return produto;
    }

    @ParameterizedTest
    @MethodSource("produtosInvalidos")
    void deveRejeitarProdutoSemAlterarEstoque(int estoque, boolean ativo, String mensagem) {
        when(usuarioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(autenticar()));
        Produto produto = produto(estoque, ativo);
        when(produtoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(produto));

        NegocioException erro = assertThrows(NegocioException.class, () -> pedidoService.criar(
                new PedidoRequest(List.of(new ItemPedidoRequest(1L, 2))), autenticado));

        assertTrue(erro.getMessage().contains(mensagem));
        assertEquals(estoque, produto.getEstoque());
        verify(produtoRepository, never()).save(any());
        verifyNoInteractions(pedidoRepository);
    }

    static Stream<Arguments> produtosInvalidos() {
        return Stream.of(Arguments.of(1, true, "Estoque insuficiente"),
                Arguments.of(10, false, "inativo"));
    }

    @Test
    void deveCancelarPedidoPendenteEDevolverEstoque() {
        Pedido pedido = new Pedido(autenticar());
        Produto produto = produto(8, true);
        when(produtoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(produto));
        pedido.getItens().add(new ItemPedido(pedido, produto, 2));
        when(pedidoRepository.findByIdAndUsuarioIdForUpdate(10L, 1L)).thenReturn(Optional.of(pedido));

        pedidoService.cancelar(10L, autenticado);

        assertEquals(10, produto.getEstoque());
        assertEquals(PedidoStatus.CANCELADO, pedido.getStatus());
        assertNotNull(pedido.getAtualizadoEm());
        verify(produtoRepository).save(produto);
        verify(pedidoRepository).save(pedido);
    }

    @ParameterizedTest
    @EnumSource(value = PedidoStatus.class, names = {"PENDENTE", "CONFIRMADO"}, mode = EnumSource.Mode.EXCLUDE)
    void deveRejeitarCancelamentoDePedidoNaoPendente(PedidoStatus status) {
        Pedido pedido = new Pedido(autenticar());
        pedido.setStatus(status);
        when(pedidoRepository.findByIdAndUsuarioIdForUpdate(10L, 1L)).thenReturn(Optional.of(pedido));

        assertThrows(NegocioException.class, () -> pedidoService.cancelar(10L, autenticado));

        assertEquals(status, pedido.getStatus());
        verify(pedidoRepository, never()).save(any());
        verifyNoInteractions(produtoRepository);
    }

    static Stream<Arguments> transicoes() {
        return Stream.of(PedidoStatus.values()).flatMap(origem -> Stream.of(PedidoStatus.values())
                .map(destino -> Arguments.of(origem, destino,
                        (origem == PedidoStatus.PENDENTE && (destino == PedidoStatus.CONFIRMADO || destino == PedidoStatus.CANCELADO))
                        || (origem == PedidoStatus.CONFIRMADO && (destino == PedidoStatus.EM_PREPARO || destino == PedidoStatus.CANCELADO))
                        || (origem == PedidoStatus.EM_PREPARO && destino == PedidoStatus.ENVIADO)
                        || (origem == PedidoStatus.ENVIADO && destino == PedidoStatus.ENTREGUE))));
    }

    @ParameterizedTest
    @MethodSource("transicoes")
    void deveRespeitarTransicoesDeStatus(PedidoStatus origem, PedidoStatus destino, boolean permitida) {
        Pedido pedido = new Pedido(autenticar());
        pedido.setStatus(origem);
        when(pedidoRepository.findByIdAndUsuarioIdForUpdate(10L, 1L)).thenReturn(Optional.of(pedido));
        if (permitida) {
            when(pedidoRepository.save(pedido)).thenReturn(pedido);
            PedidoResponse response = pedidoService.transicionarStatus(10L, new StatusRequest(destino), autenticado);
            assertEquals(destino, response.status());
            assertEquals(destino, pedido.getStatus());
            assertNotNull(pedido.getAtualizadoEm());
            verify(pedidoRepository).save(pedido);
        } else {
            assertThrows(NegocioException.class, () -> pedidoService.transicionarStatus(
                    10L, new StatusRequest(destino), autenticado));
            assertEquals(origem, pedido.getStatus());
            assertNull(pedido.getAtualizadoEm());
            verify(pedidoRepository, never()).save(any());
        }
    }

    @Test
    void deveOcultarPedidoQueNaoPertenceAoUsuario() {
        when(pedidoRepository.findByIdAndUsuarioId(10L, 1L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> pedidoService.buscarPorId(10L, autenticado));

        assertEquals(404, erro.getStatusCode().value());
        verify(pedidoRepository).findByIdAndUsuarioId(10L, 1L);
        verify(pedidoRepository, never()).findById(any());
    }
}
