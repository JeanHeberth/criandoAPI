package br.com.criandoapi.services;

import br.com.criandoapi.entity.Produto;
import br.com.criandoapi.entity.ProdutoCategoria;
import br.com.criandoapi.exception.ConflictException;
import br.com.criandoapi.record.EstoqueRequest;
import br.com.criandoapi.record.ProdutoRequest;
import br.com.criandoapi.record.ProdutoResponse;
import br.com.criandoapi.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        if (produtoRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new ConflictException("Produto com nome '" + request.nome() + "' já existe");
        }
        Produto produto = new Produto(request.nome(), request.descricao(), request.preco(), request.estoque(), request.categoria());
        return toResponse(produtoRepository.save(produto));
    }

    public Page<ProdutoResponse> listar(String nome, ProdutoCategoria categoria, BigDecimal precoMin, BigDecimal precoMax, Pageable pageable) {
        if ((precoMin != null && precoMin.signum() < 0) || (precoMax != null && precoMax.signum() < 0)
                || (precoMin != null && precoMax != null && precoMin.compareTo(precoMax) > 0)) {
            throw new ResponseStatusException(BAD_REQUEST, "Filtros de preco devem ser nao negativos e precoMin <= precoMax");
        }
        return produtoRepository.buscarComFiltros(nome, categoria, precoMin, precoMax, pageable)
                .map(this::toResponse);
    }

    public Page<ProdutoResponse> listarPorCategoria(ProdutoCategoria categoria, Pageable pageable) {
        return produtoRepository.findByCategoriaAndAtivoTrue(categoria, pageable)
                .map(this::toResponse);
    }

    public ProdutoResponse buscarPorId(Long id) {
        return toResponse(buscarOuErro(id));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = produtoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Produto nao encontrado"));

        // Verifica conflito de nome somente se mudou para outro
        if (!produto.getNome().equalsIgnoreCase(request.nome()) && produtoRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new ConflictException("Produto com nome '" + request.nome() + "' já existe");
        }

        produto.atualizar(request.nome(), request.descricao(), request.preco(), request.estoque(), request.categoria());
        return toResponse(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizarEstoque(Long id, EstoqueRequest request) {
        Produto produto = produtoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Produto nao encontrado"));
        produto.atualizarEstoque(request.estoque());
        return toResponse(produtoRepository.save(produto));
    }

    @Transactional
    public void deletar(Long id) {
        Produto produto = produtoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Produto nao encontrado"));
        // Soft delete — inativa o produto
        produto.setAtivo(false);
        produtoRepository.save(produto);
    }

    private Produto buscarOuErro(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Produto não encontrado com id: " + id));
    }

    private ProdutoResponse toResponse(Produto p) {
        return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(),
                p.getPreco(), p.getEstoque(), p.getCategoria(),
                p.getAtivo(), p.getCriadoEm(), p.getAtualizadoEm());
    }
}

