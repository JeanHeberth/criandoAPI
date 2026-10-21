package br.com.criandoapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTrackingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestTrackingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        String anterior = MDC.get("requestId");
        MDC.put("requestId", requestId);
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            // Do not log bodies, credentials, authorization headers, or query parameters.
            log.info("HTTP {} status={} durationMs={}", request.getMethod(), response.getStatus(),
                    (System.nanoTime() - inicio) / 1_000_000);
            if (anterior == null) MDC.remove("requestId"); else MDC.put("requestId", anterior);
        }
    }
}
