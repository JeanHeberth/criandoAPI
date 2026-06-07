package br.com.criandoapi.services;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.entity.UsuarioRole;
import br.com.criandoapi.record.AuthResponse;
import br.com.criandoapi.record.LoginRequest;
import br.com.criandoapi.record.RegistroRequest;
import br.com.criandoapi.repository.UsuarioRepository;
import br.com.criandoapi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Maria", "maria@email.com", "hash-salva");
        usuario.setId(1L);
        usuario.setRole(UsuarioRole.USER);
    }

    @Test
    void deveFazerLoginComCredenciaisValidas() {
        LoginRequest request = new LoginRequest("maria@email.com", "123456");

        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hash-salva")).thenReturn(true);
        when(jwtService.generateToken("maria@email.com", UsuarioRole.USER)).thenReturn("token-jwt");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("token-jwt");
        assertThat(response.usuario().email()).isEqualTo("maria@email.com");
    }

    @Test
    void deveRetornarMensagemGenericaQuandoCredenciaisInvalidas() {
        LoginRequest request = new LoginRequest("inexistente@email.com", "123456");

        when(usuarioRepository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void deveRegistrarNovoUsuarioDelegandoAoUsuarioService() {
        RegistroRequest request = new RegistroRequest("Maria", "maria@email.com", "123456");

        when(usuarioService.criarUsuario("Maria", "maria@email.com", "123456", UsuarioRole.USER)).thenReturn(usuario);
        when(jwtService.generateToken("maria@email.com", UsuarioRole.USER)).thenReturn("token-jwt");

        AuthResponse response = authService.registro(request);

        assertThat(response.token()).isEqualTo("token-jwt");
        verify(usuarioService).criarUsuario("Maria", "maria@email.com", "123456", UsuarioRole.USER);
    }
}
