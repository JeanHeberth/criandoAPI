package br.com.criandoapi.record;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProdutoUpdateRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
        String nome,

        @Size(max = 255, message = "Descricao deve ter no maximo 255 caracteres")
        String descricao,

        @NotNull(message = "Preco e obrigatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "Preco nao pode ser negativo")
        BigDecimal preco,

        @NotNull(message = "Estoque e obrigatorio")
        @PositiveOrZero(message = "Estoque nao pode ser negativo")
        Integer estoque,

        @NotNull(message = "Categoria e obrigatoria")
        Long categoriaId
) {
}

