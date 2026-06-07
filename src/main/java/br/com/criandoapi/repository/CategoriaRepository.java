package br.com.criandoapi.repository;

import br.com.criandoapi.entity.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    Page<Categoria> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    Optional<Categoria> findByNomeIgnoreCase(String nome);
}
