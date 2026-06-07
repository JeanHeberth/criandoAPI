package br.com.criandoapi.services;

import br.com.criandoapi.entity.Categoria;
import br.com.criandoapi.entity.Produto;
import br.com.criandoapi.record.PageResponse;
import br.com.criandoapi.record.ProdutoRequest;
import br.com.criandoapi.record.ProdutoResponse;
import br.com.criandoapi.record.ProdutoUpdateRequest;
import br.com.criandoapi.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private ProdutoService produtoService;

    private Categoria categoria;
    private Produto produto;

    @BeforeEach
    void setUp() {
        categoria = new Categoria("Eletronicos", "Itens eletronicos");
        categoria.setId(10L);

        produto = new Produto("Teclado", "Teclado mecanico", new BigDecimal("250.00"), 15, categoria);
        produto.setId(1L);
    }

    @Test
    void deveCriarProduto() {
        ProdutoRequest request = new ProdutoRequest(
                "Teclado",
                "Teclado mecanico",
                new BigDecimal("250.00"),
                15,
                10L
        );

        when(categoriaService.buscarEntidadePorId(10L)).thenReturn(categoria);
        when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> {
            Produto salvo = invocation.getArgument(0);
            salvo.setId(1L);
            return salvo;
        });

        ProdutoResponse response = produtoService.criar(request);

        assertThat(response.categoriaNome()).isEqualTo("Eletronicos");
    }

    @Test
    void deveLancarNotFoundAoCriarProdutoSemCategoria() {
        ProdutoRequest request = new ProdutoRequest(
                "Teclado",
                "Teclado mecanico",
                new BigDecimal("250.00"),
                15,
                999L
        );

        when(categoriaService.buscarEntidadePorId(999L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Categoria nao encontrada"));

        assertThatThrownBy(() -> produtoService.criar(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Categoria nao encontrada");
    }

    @Test
    void deveListarProdutosPaginados() {
        when(produtoRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(produto)));

        PageResponse<ProdutoResponse> response = produtoService.listar(0, 20);

        assertThat(response.conteudo()).hasSize(1);
        assertThat(response.conteudo().get(0).categoriaNome()).isEqualTo("Eletronicos");
    }

    @Test
    void deveAtualizarProduto() {
        ProdutoUpdateRequest request = new ProdutoUpdateRequest(
                "Mouse",
                "Mouse sem fio",
                new BigDecimal("130.00"),
                20,
                10L
        );

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(categoriaService.buscarEntidadePorId(10L)).thenReturn(categoria);
        when(produtoRepository.save(produto)).thenReturn(produto);

        ProdutoResponse response = produtoService.atualizar(1L, request);

        assertThat(response.nome()).isEqualTo("Mouse");
    }

    @Test
    void deveDeletarProdutoExistente() {
        when(produtoRepository.existsById(1L)).thenReturn(true);

        produtoService.deletar(1L);

        verify(produtoRepository).deleteById(1L);
    }
}
