package br.com.criandoapi.record;

import br.com.criandoapi.entity.UsuarioRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload para alterar o perfil de um usuário")
public record RoleRequest(
        @Schema(description = "Novo perfil do usuário", example = "ADMIN", allowableValues = {"USER", "ADMIN"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Role é obrigatória")
        UsuarioRole role
) {
}
