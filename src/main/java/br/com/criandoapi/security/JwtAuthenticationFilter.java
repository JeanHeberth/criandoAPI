package br.com.criandoapi.security;

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
        String path = removeApiVersionPrefix(extractPathWithoutContext(request));
        String method = request.getMethod();

        // POST /usuarios — criar conta sem token
        boolean criarUsuarioSemToken = "/usuarios".equals(path) && "POST".equalsIgnoreCase(method);

        // GET /produtos/** — listagem pública de produtos
        boolean produtosPublico = path.startsWith("/produtos") && "GET".equalsIgnoreCase(method);

        return path.startsWith("/auth/")
                || criarUsuarioSemToken
                || produtosPublico
                || path.startsWith("/actuator/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/error");
    }

    private String extractPathWithoutContext(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath == null || contextPath.isEmpty()) {
            return requestUri;
        }

        String normalizedPath = requestUri.substring(contextPath.length());
        return normalizedPath.isEmpty() ? "/" : normalizedPath;
    }

    private String removeApiVersionPrefix(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }

        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedPath.replaceFirst("^/v\\d+(?=/|$)", "");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token ausente");
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            String email = jwtService.extractUsername(token);

            if (email == null || usuarioRepository.findByEmail(email).isEmpty() || !jwtService.isTokenValid(token, email)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalido");
                return;
            }

            request.setAttribute("usuarioEmail", email);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalido");
        }
    }
}
