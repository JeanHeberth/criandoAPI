package br.com.criandoapi.security;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.record.ErroResponse;
import br.com.criandoapi.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;
    private final String apiPrefix;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository,
                                   ObjectMapper objectMapper, String apiPrefix) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
        this.apiPrefix = apiPrefix.equals("/") ? "" : apiPrefix.replaceAll("/$", "");
    }

    private String path(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!apiPrefix.isEmpty() && (path.equals(apiPrefix) || path.startsWith(apiPrefix + "/"))) {
            path = path.substring(apiPrefix.length());
        }
        return path;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = path(request);
        return path.equals("/auth/login") || path.equals("/auth/registro")
                || path.equals("/actuator/health") || path.startsWith("/actuator/health/")
                || path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/")
                || path.equals("/api-docs") || path.startsWith("/api-docs/")
                || path.equals("/v3/api-docs") || path.startsWith("/v3/api-docs/")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        // Criacao de usuario e publica; o token e opcional e so e necessario para definir role ADMIN.
        if (header == null && path(request).equals("/usuarios") && request.getMethod().equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (header == null || !header.startsWith("Bearer ") || header.length() == 7) {
            erro(request, response, 401, "Token ausente");
            return;
        }
        Usuario usuario;
        try {
            String token = header.substring(7);
            String email = jwtService.extractUsername(token);
            if (email == null || !jwtService.isTokenValid(token, email)) {
                erro(request, response, 401, "Token invalido");
                return;
            }
            usuario = usuarioRepository.findByEmail(email).orElse(null);
        } catch (JwtException | IllegalArgumentException ex) {
            erro(request, response, 401, "Token invalido");
            return;
        }
        if (usuario == null) {
            erro(request, response, 401, "Token invalido");
            return;
        }
        UsuarioAutenticado autenticado = UsuarioAutenticado.of(usuario);
        String path = path(request);
        boolean produto = path.equals("/produtos") || path.startsWith("/produtos/");
        boolean manutencao = path.equals("/manutencao") || path.startsWith("/manutencao/");
        boolean metricas = path.equals("/actuator/metrics") || path.startsWith("/actuator/metrics/");
        if (((produto && !request.getMethod().equals("GET")) || manutencao || metricas)
                && autenticado.role() != br.com.criandoapi.entity.UsuarioRole.ADMIN) {
            erro(request, response, 403, "Operacao restrita a administradores");
            return;
        }
        request.setAttribute("usuarioAutenticado", autenticado);
        // Downstream errors must retain their original status, never become invalid-token errors.
        filterChain.doFilter(request, response);
    }

    private void erro(HttpServletRequest request, HttpServletResponse response, int status, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErroResponse.of(status,
                status == 401 ? "Nao autorizado" : "Acesso negado", mensagem, request.getRequestURI()));
    }
}
