package br.com.criandoapi.record;

import br.com.criandoapi.entity.PedidoStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        Long usuarioId,
        String usuarioNome,
        List<ItemPedidoResponse> itens,
        PedidoStatus status,
        BigDecimal valorTotal,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}

