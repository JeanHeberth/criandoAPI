package br.com.criandoapi.services;

import br.com.criandoapi.entity.Categoria;
import br.com.criandoapi.entity.Produto;
import br.com.criandoapi.record.PageResponse;
import br.com.criandoapi.record.ProdutoRequest;
import br.com.criandoapi.record.ProdutoResponse;
import br.com.criandoapi.record.ProdutoUpdateRequest;
import br.com.criandoapi.repository.CategoriaRepository;
import br.com.criandoapi.repository.ProdutoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaService categoriaService;

    public ProdutoService(ProdutoRepository produtoRepository, CategoriaService categoriaService) {
        this.produtoRepository = produtoRepository;
        this.categoriaService = categoriaService;
    }

    public ProdutoResponse criar(ProdutoRequest request) {
        Categoria categoria = categoriaService.buscarEntidadePorId(request.categoriaId());

        Produto produto = new Produto(
                request.nome(),
                request.descricao(),
                request.preco(),
                request.estoque(),
                categoria
        );

        Produto produtoSalvo = produtoRepository.save(produto);
        return toResponse(produtoSalvo);
    }

    public PageResponse<ProdutoResponse> listar(int page, int size) {
        return PageResponse.of(
                produtoRepository.findAll(PageRequest.of(page, size, Sort.by("id")))
                        .map(this::toResponse)
        );
    }

    public ProdutoResponse buscarPorId(Long id) {
        Produto produto = buscarEntidadePorId(id);
        return toResponse(produto);
    }

    public PageResponse<ProdutoResponse> buscarPorNome(String nome, int page, int size) {
        return PageResponse.of(
                produtoRepository.findByNomeContainingIgnoreCase(nome, PageRequest.of(page, size, Sort.by("id")))
                        .map(this::toResponse)
        );
    }

    public PageResponse<ProdutoResponse> buscarPorCategoria(Long categoriaId, int page, int size) {
        return PageResponse.of(
                produtoRepository.findByCategoriaId(categoriaId, PageRequest.of(page, size, Sort.by("id")))
                        .map(this::toResponse)
        );
    }

    public ProdutoResponse atualizar(Long id, ProdutoUpdateRequest request) {
        Produto produto = buscarEntidadePorId(id);
        Categoria categoria = categoriaService.buscarEntidadePorId(request.categoriaId());

        produto.atualizar(
                request.nome(),
                request.descricao(),
                request.preco(),
                request.estoque(),
                categoria
        );

        Produto produtoAtualizado = produtoRepository.save(produto);
        return toResponse(produtoAtualizado);
    }

    public void deletar(Long id) {
        if (!produtoRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Produto nao encontrado com id: " + id);
        }
        produtoRepository.deleteById(id);
    }

    private Produto buscarEntidadePorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Produto nao encontrado com id: " + id));
    }

    private ProdutoResponse toResponse(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getEstoque(),
                produto.getCategoria().getId(),
                produto.getCategoria().getNome()
        );
    }
}
