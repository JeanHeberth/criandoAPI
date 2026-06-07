package br.com.criandoapi.repository;

import br.com.criandoapi.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @EntityGraph(attributePaths = "categoria")
    @Override
    Page<Produto> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    @Override
    Optional<Produto> findById(Long id);

    @EntityGraph(attributePaths = "categoria")
    Page<Produto> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Page<Produto> findByCategoriaId(Long categoriaId, Pageable pageable);

    boolean existsByCategoriaId(Long categoriaId);
}
