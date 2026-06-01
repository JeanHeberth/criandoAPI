package br.com.criandoapi.services;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.record.UsuarioRequest;
import br.com.criandoapi.record.UsuarioResponse;
import br.com.criandoapi.repository.UsuarioRepository;
import br.com.criandoapi.security.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UsuarioResponse criarUsuario(UsuarioRequest usuarioRequest) {
        Usuario usuario = new Usuario(usuarioRequest.nome(), usuarioRequest.email(), PasswordUtils.hash(usuarioRequest.senha()));
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        return new UsuarioResponse(usuarioSalvo.getId(), usuarioSalvo.getNome(), usuarioSalvo.getEmail());
    }

    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(u -> new UsuarioResponse(u.getId(), u.getNome(), u.getEmail()))
                .toList();
    }

    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario nao encontrado com id: " + id));
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }

    public List<UsuarioResponse> buscarPorNome(String nome) {
        return usuarioRepository.findByNomeContainingIgnoreCase(nome)
                .stream()
                .map(u -> new UsuarioResponse(u.getId(), u.getNome(), u.getEmail()))
                .toList();
    }

    public UsuarioResponse atualizarUsuario(Long id, UsuarioRequest usuarioRequest) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario nao encontrado com id: " + id));
        usuario.atualizar(usuarioRequest.nome(), usuarioRequest.email(), PasswordUtils.hash(usuarioRequest.senha()));
        Usuario usuarioAtualizado = usuarioRepository.save(usuario);
        return new UsuarioResponse(usuarioAtualizado.getId(), usuarioAtualizado.getNome(), usuarioAtualizado.getEmail());
    }

    public void deletarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Usuario nao encontrado com id: " + id);
        }
        usuarioRepository.deleteById(id);
    }
}

