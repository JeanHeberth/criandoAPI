package br.com.criandoapi.record;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Item de um pedido com produto e quantidade")
public record ItemPedidoRequest(
        @Schema(description = "ID do produto a ser incluído no pedido (deve existir e estar ativo)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "ID do produto é obrigatório")
        Long produtoId,

        @Schema(description = "Quantidade desejada do produto (mínimo 1, não pode exceder o estoque disponível)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
        Integer quantidade
) {
}
