package br.com.criandoapi.services;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.record.AuthResponse;
import br.com.criandoapi.record.LoginRequest;
import br.com.criandoapi.record.UsuarioResponse;
import br.com.criandoapi.repository.UsuarioRepository;
import br.com.criandoapi.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import br.com.criandoapi.security.PasswordUtils;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }


    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Usuario nao encontrado com email: " + request.email()));

        if (!PasswordUtils.matches(request.senha(), usuario.getSenha())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Senha invalida");
        }

        String token = jwtService.generateToken(usuario.getEmail());

        return new AuthResponse(token, "Bearer", new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail()));
    }
}

