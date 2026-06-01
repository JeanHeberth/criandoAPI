package br.com.criandoapi.repository;

import br.com.criandoapi.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findByNomeContainingIgnoreCase(String nome);

    Optional<Usuario> findByEmail(String email);
}

