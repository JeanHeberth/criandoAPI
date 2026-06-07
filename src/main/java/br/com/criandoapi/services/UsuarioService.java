package br.com.criandoapi.services;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.entity.UsuarioRole;
import br.com.criandoapi.record.PageResponse;
import br.com.criandoapi.record.UsuarioAdminRequest;
import br.com.criandoapi.record.UsuarioResponse;
import br.com.criandoapi.record.UsuarioUpdateRequest;
import br.com.criandoapi.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario criarUsuario(String nome, String email, String senha, UsuarioRole role) {
        validarEmailDisponivel(email);

        Usuario usuario = new Usuario(nome, email, passwordEncoder.encode(senha));
        usuario.setRole(role);
        return usuarioRepository.save(usuario);
    }

    public UsuarioResponse criarUsuarioAdmin(UsuarioAdminRequest request) {
        Usuario usuarioSalvo = criarUsuario(request.nome(), request.email(), request.senha(), request.role());
        return toResponse(usuarioSalvo);
    }

    public PageResponse<UsuarioResponse> listar(int page, int size) {
        return PageResponse.of(
                usuarioRepository.findAll(PageRequest.of(page, size, Sort.by("id")))
                        .map(this::toResponse)
        );
    }

    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        return toResponse(usuario);
    }

    public PageResponse<UsuarioResponse> buscarPorNome(String nome, int page, int size) {
        return PageResponse.of(
                usuarioRepository.findByNomeContainingIgnoreCase(nome, PageRequest.of(page, size, Sort.by("id")))
                        .map(this::toResponse)
        );
    }

    public UsuarioResponse atualizarUsuario(Long id, UsuarioUpdateRequest usuarioRequest) {
        Usuario usuario = buscarEntidadePorId(id);
        validarEmailDisponivelParaAtualizacao(id, usuarioRequest.email());

        String senha = usuario.getSenha();
        if (usuarioRequest.senha() != null && !usuarioRequest.senha().isBlank()) {
            senha = passwordEncoder.encode(usuarioRequest.senha());
        }

        usuario.atualizar(usuarioRequest.nome(), usuarioRequest.email(), senha);
        Usuario usuarioAtualizado = usuarioRepository.save(usuario);
        return toResponse(usuarioAtualizado);
    }

    public void deletarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Usuario nao encontrado com id: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    private Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario nao encontrado com id: " + id));
    }

    private void validarEmailDisponivel(String email) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Email ja cadastrado");
        }
    }

    private void validarEmailDisponivelParaAtualizacao(Long id, String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuarioExistente -> {
            if (!usuarioExistente.getId().equals(id)) {
                throw new ResponseStatusException(CONFLICT, "Email ja cadastrado");
            }
        });
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
