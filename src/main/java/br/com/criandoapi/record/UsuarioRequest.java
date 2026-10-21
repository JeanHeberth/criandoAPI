package br.com.criandoapi.record;

import io.swagger.v3.oas.annotations.media.Schema;
import br.com.criandoapi.entity.UsuarioRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para criação ou atualização de usuário")
public record UsuarioRequest(
        @Schema(description = "Nome completo do usuário (3 a 100 caracteres)", example = "João da Silva", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
        String nome,

        @Schema(description = "E-mail único do usuário — deve ser um e-mail válido e não cadastrado", example = "joao@email.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail deve ter formato válido")
        String email,

        @Schema(description = "Senha de acesso (mínimo 6 caracteres)", example = "senha123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String senha,

        @Schema(description = "Perfil do usuário (USER ou ADMIN). Opcional, padrão USER. Definir ADMIN exige token de administrador; ignorado na atualização (PUT)", example = "USER", allowableValues = {"USER", "ADMIN"})
        UsuarioRole role
) {
    public UsuarioRequest(String nome, String email, String senha) {
        this(nome, email, senha, null);
    }
}
