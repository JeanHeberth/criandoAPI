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
        LocalDateTime timestamp,

        @Schema(description = "Codigo estavel para tratamento pelo cliente", example = "RECURSO_NAO_ENCONTRADO")
        String codigo,

        @Schema(description = "Identificador de correlacao retornado em X-Request-ID")
        String requestId,

        java.util.List<ErroValidacaoResponse.CampoErro> campos
) {
    public static String codigo(int status) {
        return switch (status) {
            case 400 -> "REQUISICAO_INVALIDA";
            case 401 -> "NAO_AUTENTICADO";
            case 403 -> "ACESSO_NEGADO";
            case 404 -> "RECURSO_NAO_ENCONTRADO";
            case 405 -> "METODO_NAO_PERMITIDO";
            case 409 -> "CONFLITO";
            case 415 -> "TIPO_DE_CONTEUDO_INVALIDO";
            case 422 -> "REGRA_DE_NEGOCIO";
            default -> "ERRO_INTERNO";
        };
    }

    public static ErroResponse of(int status, String erro, String mensagem, String path) {
        return new ErroResponse(status, erro, mensagem, path, LocalDateTime.now(java.time.Clock.systemUTC()),
                codigo(status), org.slf4j.MDC.get("requestId"), java.util.List.of());
    }
}
