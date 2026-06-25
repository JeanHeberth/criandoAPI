package br.com.criandoapi.record;

import br.com.criandoapi.entity.ProdutoCategoria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Payload para criação ou atualização de produto")
public record ProdutoRequest(
        @Schema(description = "Nome do produto (2 a 150 caracteres, único no sistema)", example = "Notebook Dell Inspiron 15", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
        String nome,

        @Schema(description = "Descrição detalhada do produto (até 500 caracteres)", example = "Notebook com processador Intel i7, 16GB RAM, SSD 512GB")
        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        String descricao,

        @Schema(description = "Preço unitário do produto (deve ser maior que zero)", example = "3499.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal preco,

        @Schema(description = "Quantidade disponível em estoque (não pode ser negativa)", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Estoque é obrigatório")
        @Min(value = 0, message = "Estoque não pode ser negativo")
        Integer estoque,

        @Schema(description = "Categoria do produto. Valores aceitos: ELETRONICO, VESTUARIO, ALIMENTO, LIVRO, OUTRO", example = "ELETRONICO", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Categoria é obrigatória")
        ProdutoCategoria categoria
) {
}
