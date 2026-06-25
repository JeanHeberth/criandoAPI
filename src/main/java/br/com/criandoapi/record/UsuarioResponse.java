package br.com.criandoapi.record;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de resposta do usuário (sem senha)")
public record UsuarioResponse(
        @Schema(description = "Identificador único do usuário", example = "1")
        Long id,

        @Schema(description = "Nome completo do usuário", example = "João da Silva")
        String nome,

        @Schema(description = "E-mail do usuário", example = "joao@email.com")
        String email
) {
}
