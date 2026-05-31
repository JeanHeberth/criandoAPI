package br.com.criandoapi.services;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.record.UsuarioRequest;
import br.com.criandoapi.record.UsuarioResponse;
import br.com.criandoapi.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UsuarioResponse criarUsuario(UsuarioRequest usuarioRequest) {
        Usuario usuario = new Usuario(usuarioRequest.nome(), usuarioRequest.email());
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
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado com id: " + id));
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }

    public UsuarioResponse atualizarUsuario(Long id, UsuarioRequest usuarioRequest) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado com id: " + id));
        usuario.atualizar(usuarioRequest.nome(), usuarioRequest.email());
        Usuario usuarioAtualizado = usuarioRepository.save(usuario);
        return new UsuarioResponse(usuarioAtualizado.getId(), usuarioAtualizado.getNome(), usuarioAtualizado.getEmail());
    }

    public void deletarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario nao encontrado com id: " + id);
        }
        usuarioRepository.deleteById(id);
    }
}

