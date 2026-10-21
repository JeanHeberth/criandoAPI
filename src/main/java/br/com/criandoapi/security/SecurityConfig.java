package br.com.criandoapi.security;

import br.com.criandoapi.repository.UsuarioRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Value("${api.version-prefix}") String prefix) {
        return new JwtAuthenticationFilter(jwtService, usuarioRepository, objectMapper, prefix);
    }


    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}

