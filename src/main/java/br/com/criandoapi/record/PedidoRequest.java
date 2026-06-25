package br.com.criandoapi.record;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Payload para criação de pedido com um ou mais itens")
public record PedidoRequest(
        @Schema(description = "Lista de itens do pedido. Deve conter pelo menos 1 item. O estoque será baixado automaticamente.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "O pedido deve ter pelo menos um item")
        @Valid
        List<ItemPedidoRequest> itens
) {
}
