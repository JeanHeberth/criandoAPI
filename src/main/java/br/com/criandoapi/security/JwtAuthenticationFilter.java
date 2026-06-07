package br.com.criandoapi.security;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            JsonErrorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Token ausente");
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            String email = jwtService.extractUsername(token);
            Usuario usuario = email != null ? usuarioRepository.findByEmail(email).orElse(null) : null;

            if (usuario == null || !jwtService.isTokenValid(token, email)) {
                JsonErrorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Token invalido");
                return;
            }

            AuthenticatedUsuario authenticatedUser = new AuthenticatedUsuario(
                    usuario.getId(),
                    usuario.getEmail(),
                    usuario.getRole()
            );
            request.setAttribute(RoleAuthorizationInterceptor.AUTHENTICATED_USER_ATTR, authenticatedUser);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            JsonErrorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Token invalido");
        }
    }
}
