package br.com.criandoapi.record;

import br.com.criandoapi.entity.ProdutoCategoria;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Dados de resposta do produto")
public record ProdutoResponse(
        @Schema(description = "Identificador único do produto", example = "1")
        Long id,

        @Schema(description = "Nome do produto", example = "Notebook Dell Inspiron 15")
        String nome,

        @Schema(description = "Descrição detalhada do produto", example = "Notebook com processador Intel i7, 16GB RAM, SSD 512GB")
        String descricao,

        @Schema(description = "Preço unitário do produto", example = "3499.99")
        BigDecimal preco,

        @Schema(description = "Quantidade disponível em estoque", example = "50")
        Integer estoque,

        @Schema(description = "Categoria do produto", example = "ELETRONICO")
        ProdutoCategoria categoria,

        @Schema(description = "Indica se o produto está ativo (false = soft deleted)", example = "true")
        Boolean ativo,

        @Schema(description = "Data e hora de criação do produto", example = "2026-06-21T10:00:00")
        LocalDateTime criadoEm,

        @Schema(description = "Data e hora da última atualização do produto", example = "2026-06-21T10:00:00")
        LocalDateTime atualizadoEm
) {
}
