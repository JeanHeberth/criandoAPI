package br.com.criandoapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private Integer estoque = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProdutoCategoria categoria;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column
    private LocalDateTime atualizadoEm;

    public Produto(String nome, String descricao, BigDecimal preco, Integer estoque, ProdutoCategoria categoria) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque;
        this.categoria = categoria;
    }

    public void atualizar(String nome, String descricao, BigDecimal preco, Integer estoque, ProdutoCategoria categoria) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque;
        this.categoria = categoria;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void atualizarEstoque(Integer quantidade) {
        this.estoque = quantidade;
        this.atualizadoEm = LocalDateTime.now();
    }
}

