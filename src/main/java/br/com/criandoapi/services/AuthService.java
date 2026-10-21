package br.com.criandoapi.services;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.exception.ConflictException;
import br.com.criandoapi.record.AuthResponse;
import br.com.criandoapi.record.LoginRequest;
import br.com.criandoapi.record.RegistroRequest;
import br.com.criandoapi.record.UsuarioResponse;
import br.com.criandoapi.repository.UsuarioRepository;
import br.com.criandoapi.security.JwtService;
import br.com.criandoapi.security.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Credenciais inválidas"));

        if (!PasswordUtils.matches(request.senha(), usuario.getSenha())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Credenciais inválidas");
        }

        String token = jwtService.generateToken(usuario.getEmail());
        return new AuthResponse(token, "Bearer", new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail()));
    }

    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("E-mail '" + request.email() + "' já está em uso");
        }

        Usuario usuario = new Usuario(request.nome(), request.email(), PasswordUtils.hash(request.senha()));
        Usuario salvo = usuarioRepository.save(usuario);

        String token = jwtService.generateToken(salvo.getEmail());
        return new AuthResponse(token, "Bearer", new UsuarioResponse(salvo.getId(), salvo.getNome(), salvo.getEmail()));
    }
}
