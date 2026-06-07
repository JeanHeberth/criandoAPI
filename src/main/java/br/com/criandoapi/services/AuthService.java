package br.com.criandoapi.services;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.entity.UsuarioRole;
import br.com.criandoapi.record.AuthResponse;
import br.com.criandoapi.record.LoginRequest;
import br.com.criandoapi.record.RegistroRequest;
import br.com.criandoapi.record.UsuarioResponse;
import br.com.criandoapi.repository.UsuarioRepository;
import br.com.criandoapi.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "Credenciais invalidas";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository,
                       UsuarioService usuarioService,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email()).orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new ResponseStatusException(UNAUTHORIZED, CREDENCIAIS_INVALIDAS);
        }

        return gerarAuthResponse(usuario);
    }

    public AuthResponse registro(RegistroRequest request) {
        Usuario usuarioSalvo = usuarioService.criarUsuario(
                request.nome(),
                request.email(),
                request.senha(),
                UsuarioRole.USER
        );
        return gerarAuthResponse(usuarioSalvo);
    }

    private AuthResponse gerarAuthResponse(Usuario usuario) {
        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRole());
        UsuarioResponse usuarioResponse = new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
        return new AuthResponse(token, "Bearer", usuarioResponse);
    }
}
