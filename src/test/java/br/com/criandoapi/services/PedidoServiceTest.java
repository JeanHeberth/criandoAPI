package br.com.criandoapi.services;

import br.com.criandoapi.entity.Produto;
import br.com.criandoapi.entity.ProdutoCategoria;
import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.record.ItemPedidoRequest;
import br.com.criandoapi.record.PedidoRequest;
import br.com.criandoapi.repository.PedidoRepository;
import br.com.criandoapi.repository.ProdutoRepository;
import br.com.criandoapi.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

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

        when(httpServletRequest.getAttribute("usuarioEmail")).thenReturn("teste@api.com");
        when(usuarioRepository.findByEmail("teste@api.com")).thenReturn(Optional.of(usuario));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));

        when(pedidoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> pedidoService.criar(request, httpServletRequest));

        verify(pedidoRepository).save(any());
    }
}

