package br.com.criandoapi.services;

import br.com.criandoapi.entity.Categoria;
import br.com.criandoapi.record.CategoriaRequest;
import br.com.criandoapi.record.CategoriaResponse;
import br.com.criandoapi.record.CategoriaUpdateRequest;
import br.com.criandoapi.record.PageResponse;
import br.com.criandoapi.repository.CategoriaRepository;
import br.com.criandoapi.repository.ProdutoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProdutoRepository produtoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }

    public CategoriaResponse criar(CategoriaRequest request) {
        validarNomeDisponivel(request.nome(), null);

        Categoria categoria = new Categoria(request.nome(), request.descricao());
        Categoria categoriaSalva = categoriaRepository.save(categoria);
        return toResponse(categoriaSalva);
    }

    public PageResponse<CategoriaResponse> listar(int page, int size) {
        return PageResponse.of(
                categoriaRepository.findAll(PageRequest.of(page, size, Sort.by("id")))
                        .map(this::toResponse)
        );
    }

    public CategoriaResponse buscarPorId(Long id) {
        Categoria categoria = buscarEntidadePorId(id);
        return toResponse(categoria);
    }

    public PageResponse<CategoriaResponse> buscarPorNome(String nome, int page, int size) {
        return PageResponse.of(
                categoriaRepository.findByNomeContainingIgnoreCase(nome, PageRequest.of(page, size, Sort.by("id")))
                        .map(this::toResponse)
        );
    }

    public CategoriaResponse atualizar(Long id, CategoriaUpdateRequest request) {
        Categoria categoria = buscarEntidadePorId(id);
        validarNomeDisponivel(request.nome(), id);

        categoria.atualizar(request.nome(), request.descricao());
        Categoria categoriaAtualizada = categoriaRepository.save(categoria);
        return toResponse(categoriaAtualizada);
    }

    public void deletar(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Categoria nao encontrada com id: " + id);
        }

        if (produtoRepository.existsByCategoriaId(id)) {
            throw new ResponseStatusException(CONFLICT, "Categoria possui produtos vinculados");
        }

        categoriaRepository.deleteById(id);
    }

    Categoria buscarEntidadePorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Categoria nao encontrada com id: " + id));
    }

    private void validarNomeDisponivel(String nome, Long idAtual) {
        categoriaRepository.findByNomeIgnoreCase(nome).ifPresent(categoriaExistente -> {
            if (idAtual == null || !categoriaExistente.getId().equals(idAtual)) {
                throw new ResponseStatusException(CONFLICT, "Nome da categoria ja cadastrado");
            }
        });
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getDescricao());
    }
}
