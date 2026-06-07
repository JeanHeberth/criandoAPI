package br.com.criandoapi.repository;

import br.com.criandoapi.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Page<Usuario> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    Optional<Usuario> findByEmail(String email);
}
