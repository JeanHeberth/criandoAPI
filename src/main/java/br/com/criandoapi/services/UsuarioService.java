package br.com.criandoapi.services;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.exception.ConflictException;
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
        // 409 - Conflito: e-mail já cadastrado
        if (usuarioRepository.findByEmail(usuarioRequest.email()).isPresent()) {
            throw new ConflictException("E-mail '" + usuarioRequest.email() + "' já está em uso");
        }
        Usuario usuario = new Usuario(usuarioRequest.nome(), usuarioRequest.email(), PasswordUtils.hash(usuarioRequest.senha()));
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        return toResponse(usuarioSalvo);
    }

    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario nao encontrado com id: " + id));
        return toResponse(usuario);
    }

    public List<UsuarioResponse> buscarPorNome(String nome) {
        return usuarioRepository.findByNomeContainingIgnoreCase(nome)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UsuarioResponse atualizarUsuario(Long id, UsuarioRequest usuarioRequest) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario nao encontrado com id: " + id));

        // Verifica conflito de e-mail somente se mudou para outro
        usuarioRepository.findByEmail(usuarioRequest.email())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> { throw new ConflictException("E-mail '" + usuarioRequest.email() + "' já está em uso"); });

        usuario.atualizar(usuarioRequest.nome(), usuarioRequest.email(), PasswordUtils.hash(usuarioRequest.senha()));
        return toResponse(usuarioRepository.save(usuario));
    }

    public void deletarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Usuario nao encontrado com id: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail());
    }
}

