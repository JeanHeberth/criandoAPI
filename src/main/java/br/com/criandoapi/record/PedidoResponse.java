package br.com.criandoapi.record;

import br.com.criandoapi.entity.PedidoStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Dados de resposta de um pedido")
public record PedidoResponse(
        @Schema(description = "Identificador único do pedido", example = "1")
        Long id,

        @Schema(description = "ID do usuário dono do pedido", example = "1")
        Long usuarioId,

        @Schema(description = "Nome do usuário dono do pedido", example = "João da Silva")
        String usuarioNome,

        @Schema(description = "Lista de itens do pedido com preços calculados")
        List<ItemPedidoResponse> itens,

        @Schema(description = "Status atual do pedido. Fluxo: PENDENTE → CONFIRMADO → EM_PREPARO → ENVIADO → ENTREGUE. Cancelamento: PENDENTE ou CONFIRMADO → CANCELADO", example = "PENDENTE")
        PedidoStatus status,

        @Schema(description = "Valor total do pedido (soma de precoUnitario × quantidade de cada item)", example = "6999.98")
        BigDecimal valorTotal,

        @Schema(description = "Data e hora de criação do pedido", example = "2026-06-21T10:00:00")
        LocalDateTime criadoEm,

        @Schema(description = "Data e hora da última atualização do pedido", example = "2026-06-21T10:00:00")
        LocalDateTime atualizadoEm
) {
}
