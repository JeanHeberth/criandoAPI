package br.com.criandoapi.repository;

import br.com.criandoapi.entity.Pedido;
import br.com.criandoapi.entity.PedidoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Page<Pedido> findByUsuarioId(Long usuarioId, Pageable pageable);

    Page<Pedido> findByUsuarioIdAndStatus(Long usuarioId, PedidoStatus status, Pageable pageable);

    Optional<Pedido> findByIdAndUsuarioId(Long id, Long usuarioId);
    Optional<Pedido> findByUsuarioIdAndIdempotencyKeyHash(Long usuarioId, String idempotencyKeyHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id and p.usuario.id = :usuarioId")
    Optional<Pedido> findByIdAndUsuarioIdForUpdate(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}

