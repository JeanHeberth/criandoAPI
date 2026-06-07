package br.com.criandoapi.security;

import br.com.criandoapi.entity.UsuarioRole;

public record AuthenticatedUsuario(Long id, String email, UsuarioRole role) {
}
