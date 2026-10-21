package br.com.criandoapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pedidos", uniqueConstraints = @UniqueConstraint(
        name = "uk_pedido_usuario_idempotencia", columnNames = {"usuario_id", "idempotency_key_hash"}))
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PedidoStatus status = PedidoStatus.PENDENTE;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column
    private LocalDateTime atualizadoEm;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "idempotency_key_hash", length = 64)
    private String idempotencyKeyHash;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    public Pedido(Usuario usuario) {
        this.usuario = usuario;
    }

    public void calcularTotal() {
        this.valorTotal = itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void transicionarStatus(PedidoStatus novoStatus) {
        this.status = novoStatus;
        this.atualizadoEm = LocalDateTime.now();
    }
}

