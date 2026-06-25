package br.com.criandoapi.record;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de autenticação com token JWT")
public record AuthResponse(
        @Schema(description = "Token JWT gerado, válido por 24 horas", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2FvQGVtYWlsLmNvbSIsImlhdCI6MTcxOTAwMDAwMCwiZXhwIjoxNzE5MDg2NDAwfQ.assinatura")
        String token,

        @Schema(description = "Tipo do token — sempre 'Bearer'", example = "Bearer")
        String tipo,

        @Schema(description = "Dados resumidos do usuário autenticado")
        UsuarioResponse usuario
) {
}
