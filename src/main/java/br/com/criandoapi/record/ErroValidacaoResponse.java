package br.com.criandoapi.record;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Padrão de resposta de erro de validação (400 Bad Request)")
public record ErroValidacaoResponse(
        @Schema(description = "Código HTTP do erro", example = "400")
        int status,

        @Schema(description = "Tipo do erro", example = "Erro de Validação")
        String erro,

        @Schema(description = "Mensagem geral do erro", example = "Um ou mais campos estão inválidos")
        String mensagem,

        @Schema(description = "Caminho da requisição que gerou o erro", example = "/produtos")
        String path,

        @Schema(description = "Data e hora em que o erro ocorreu", example = "2026-06-21T10:00:00")
        LocalDateTime timestamp,

        @Schema(description = "Lista dos campos com erro e suas respectivas mensagens")
        List<CampoErro> campos
) {
    @Schema(description = "Detalhe de validação por campo")
    public record CampoErro(
            @Schema(description = "Nome do campo com erro", example = "preco")
            String campo,

            @Schema(description = "Mensagem de validação do campo", example = "Preço deve ser maior que zero")
            String mensagem
    ) {}

    public static ErroValidacaoResponse of(int status, String path, List<CampoErro> campos) {
        return new ErroValidacaoResponse(
                status,
                "Erro de Validação",
                "Um ou mais campos estão inválidos",
                path,
                LocalDateTime.now(),
                campos
        );
    }
}
