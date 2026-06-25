package br.com.criandoapi.record;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Padrão de resposta de erro da API")
public record ErroResponse(
        @Schema(description = "Código HTTP do erro", example = "404")
        int status,

        @Schema(description = "Tipo do erro", example = "Recurso Não Encontrado")
        String erro,

        @Schema(description = "Mensagem descritiva do erro", example = "Produto com id 99 não encontrado")
        String mensagem,

        @Schema(description = "Caminho da requisição que gerou o erro", example = "/produtos/99")
        String path,

        @Schema(description = "Data e hora em que o erro ocorreu", example = "2026-06-21T10:00:00")
        LocalDateTime timestamp
) {
    public static ErroResponse of(int status, String erro, String mensagem, String path) {
        return new ErroResponse(status, erro, mensagem, path, LocalDateTime.now());
    }
}
