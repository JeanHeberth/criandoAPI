package br.com.criandoapi.security;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.entity.UsuarioRole;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record UsuarioAutenticado(Long id, String email, UsuarioRole role) {
    public static UsuarioAutenticado of(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getRole());
    }

    public void exigirAdministrador() {
        if (role != UsuarioRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operacao restrita a administradores");
        }
    }

    public void exigirProprioUsuarioOuAdministrador(Long usuarioId) {
        if (role != UsuarioRole.ADMIN && !id.equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
    }
}
