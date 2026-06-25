package br.com.criandoapi.record;

import br.com.criandoapi.entity.PedidoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload para transição de status do pedido")
public record StatusRequest(
        @Schema(
                description = "Novo status desejado para o pedido. Transições válidas: PENDENTE → CONFIRMADO → EM_PREPARO → ENVIADO → ENTREGUE. Cancelamento permitido de: PENDENTE ou CONFIRMADO → CANCELADO",
                example = "CONFIRMADO",
                allowableValues = {"PENDENTE", "CONFIRMADO", "EM_PREPARO", "ENVIADO", "ENTREGUE", "CANCELADO"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Novo status é obrigatório")
        PedidoStatus status
) {
}
