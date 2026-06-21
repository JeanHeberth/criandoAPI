package br.com.criandoapi.repository;

import br.com.criandoapi.entity.Pedido;
import br.com.criandoapi.entity.PedidoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Page<Pedido> findByUsuarioId(Long usuarioId, Pageable pageable);

    Page<Pedido> findByUsuarioIdAndStatus(Long usuarioId, PedidoStatus status, Pageable pageable);

    Optional<Pedido> findByIdAndUsuarioId(Long id, Long usuarioId);
}

