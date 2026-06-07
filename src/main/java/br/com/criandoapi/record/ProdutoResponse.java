package br.com.criandoapi.record;

import java.math.BigDecimal;

public record ProdutoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer estoque,
        Long categoriaId,
        String categoriaNome
) {
}

