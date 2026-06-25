package br.com.criandoapi.record;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload de autenticação do usuário")
public record LoginRequest(

        @Schema(description = "E-mail cadastrado do usuário", example = "joao@email.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail deve ter formato válido")
        String email,

        @Schema(description = "Senha do usuário (mínimo 6 caracteres)", example = "senha123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Senha é obrigatória")
        String senha
) {
}
