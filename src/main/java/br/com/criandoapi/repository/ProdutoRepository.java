package br.com.criandoapi.repository;

import br.com.criandoapi.entity.Produto;
import br.com.criandoapi.entity.ProdutoCategoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Page<Produto> findByAtivoTrue(Pageable pageable);

    Page<Produto> findByCategoriaAndAtivoTrue(ProdutoCategoria categoria, Pageable pageable);

    boolean existsByNomeIgnoreCase(String nome);

    @Query("""
            SELECT p FROM Produto p
            WHERE p.ativo = true
              AND (:nome IS NULL OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
              AND (:categoria IS NULL OR p.categoria = :categoria)
              AND (:precoMin IS NULL OR p.preco >= :precoMin)
              AND (:precoMax IS NULL OR p.preco <= :precoMax)
            """)
    Page<Produto> buscarComFiltros(
            @Param("nome") String nome,
            @Param("categoria") ProdutoCategoria categoria,
            @Param("precoMin") BigDecimal precoMin,
            @Param("precoMax") BigDecimal precoMax,
            Pageable pageable
    );
}

