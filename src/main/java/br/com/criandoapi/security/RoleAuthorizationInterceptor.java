package br.com.criandoapi.security;

import br.com.criandoapi.entity.UsuarioRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    public static final String AUTHENTICATED_USER_ATTR = "authenticatedUser";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresRole requiresRole = handlerMethod.getMethodAnnotation(RequiresRole.class);
        if (requiresRole == null) {
            requiresRole = handlerMethod.getBeanType().getAnnotation(RequiresRole.class);
        }

        if (requiresRole == null) {
            return true;
        }

        AuthenticatedUsuario authenticatedUser = (AuthenticatedUsuario) request.getAttribute(AUTHENTICATED_USER_ATTR);
        if (authenticatedUser == null) {
            throw new ResponseStatusException(FORBIDDEN, "Acesso negado");
        }

        boolean allowed = Arrays.stream(requiresRole.value())
                .anyMatch(role -> role == authenticatedUser.role());

        if (!allowed) {
            throw new ResponseStatusException(FORBIDDEN, "Acesso negado");
        }

        return true;
    }
}
