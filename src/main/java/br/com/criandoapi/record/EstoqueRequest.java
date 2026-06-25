package br.com.criandoapi.record;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload para atualização parcial do estoque de um produto")
public record EstoqueRequest(
        @Schema(description = "Nova quantidade em estoque (0 = sem estoque, não aceita negativos)", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 0, message = "Quantidade não pode ser negativa")
        Integer quantidade
) {
}
