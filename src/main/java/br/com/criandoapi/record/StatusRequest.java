package br.com.criandoapi.record;

import br.com.criandoapi.entity.PedidoStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull(message = "Novo status é obrigatório")
        PedidoStatus status
) {
}

