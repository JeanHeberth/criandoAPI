package br.com.criandoapi.record;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Dados de resposta de um item do pedido")
public record ItemPedidoResponse(
        @Schema(description = "Identificador único do item", example = "1")
        Long id,

        @Schema(description = "ID do produto referenciado", example = "1")
        Long produtoId,

        @Schema(description = "Nome do produto no momento do pedido", example = "Notebook Dell Inspiron 15")
        String produtoNome,

        @Schema(description = "Quantidade solicitada do produto", example = "2")
        Integer quantidade,

        @Schema(description = "Preço unitário do produto no momento do pedido", example = "3499.99")
        BigDecimal precoUnitario,

        @Schema(description = "Subtotal do item (precoUnitario × quantidade)", example = "6999.98")
        BigDecimal subtotal
) {
}
