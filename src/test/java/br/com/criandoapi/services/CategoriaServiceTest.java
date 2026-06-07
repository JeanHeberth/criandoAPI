package br.com.criandoapi.services;

import br.com.criandoapi.entity.Categoria;
import br.com.criandoapi.record.CategoriaRequest;
import br.com.criandoapi.record.CategoriaResponse;
import br.com.criandoapi.record.CategoriaUpdateRequest;
import br.com.criandoapi.record.PageResponse;
import br.com.criandoapi.repository.CategoriaRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = new Categoria("Eletronicos", "Itens eletronicos");
        categoria.setId(1L);
    }

    @Test
    void deveCriarCategoria() {
        CategoriaRequest request = new CategoriaRequest("Eletronicos", "Itens eletronicos");

        when(categoriaRepository.findByNomeIgnoreCase("Eletronicos")).thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocation -> {
            Categoria salva = invocation.getArgument(0);
            salva.setId(1L);
            return salva;
        });

        CategoriaResponse response = categoriaService.criar(request);

        assertThat(response.nome()).isEqualTo("Eletronicos");
    }

    @Test
    void deveLancarConflitoAoDeletarCategoriaComProdutos() {
        when(categoriaRepository.existsById(1L)).thenReturn(true);
        when(produtoRepository.existsByCategoriaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.deletar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Categoria possui produtos vinculados");

        verify(categoriaRepository, never()).deleteById(1L);
    }

    @Test
    void deveDeletarCategoriaSemProdutos() {
        when(categoriaRepository.existsById(1L)).thenReturn(true);
        when(produtoRepository.existsByCategoriaId(1L)).thenReturn(false);

        categoriaService.deletar(1L);

        verify(categoriaRepository).deleteById(1L);
    }

    @Test
    void deveListarCategoriasPaginadas() {
        when(categoriaRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(categoria)));

        PageResponse<CategoriaResponse> response = categoriaService.listar(0, 20);

        assertThat(response.conteudo()).hasSize(1);
    }

    @Test
    void deveAtualizarCategoria() {
        CategoriaUpdateRequest request = new CategoriaUpdateRequest("Casa", "Utilidades");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.findByNomeIgnoreCase("Casa")).thenReturn(Optional.empty());
        when(categoriaRepository.save(categoria)).thenReturn(categoria);

        CategoriaResponse response = categoriaService.atualizar(1L, request);

        assertThat(response.nome()).isEqualTo("Casa");
    }
}
