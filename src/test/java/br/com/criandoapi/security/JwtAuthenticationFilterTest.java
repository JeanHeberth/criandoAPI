package br.com.criandoapi.security;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.entity.UsuarioRole;
import br.com.criandoapi.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Maria", "maria@email.com", "hash");
        usuario.setId(1L);
        usuario.setRole(UsuarioRole.USER);
    }

    @Test
    void devePermitirRotaPublicaDeLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isTrue();
        jwtAuthenticationFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void deveBloquearRequisicaoSemTokenComJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/usuarios");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("Token ausente");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void deveAutorizarRequisicaoComTokenValido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/usuarios");
        request.addHeader("Authorization", "Bearer token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("token-valido")).thenReturn("maria@email.com");
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario));
        when(jwtService.isTokenValid("token-valido", "maria@email.com")).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        AuthenticatedUsuario authenticatedUser = (AuthenticatedUsuario) request.getAttribute(
                RoleAuthorizationInterceptor.AUTHENTICATED_USER_ATTR
        );
        assertThat(authenticatedUser.email()).isEqualTo("maria@email.com");
        assertThat(authenticatedUser.role()).isEqualTo(UsuarioRole.USER);
        verify(filterChain).doFilter(request, response);
    }
}
