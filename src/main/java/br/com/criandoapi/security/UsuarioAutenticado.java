package br.com.criandoapi.security;

import br.com.criandoapi.entity.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record UsuarioAutenticado(Long id, String email) {
    public static UsuarioAutenticado of(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getEmail());
    }

    public void exigirProprioUsuario(Long usuarioId) {
        if (!id.equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
    }
}
