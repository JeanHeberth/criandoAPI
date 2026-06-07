package br.com.criandoapi.record;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequest(
        @NotBlank(message = "Nome e obrigatorio")
        String nome,

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        String email,

        @Size(min = 6, message = "Senha deve ter no minimo 6 caracteres")
        String senha
) {
}
