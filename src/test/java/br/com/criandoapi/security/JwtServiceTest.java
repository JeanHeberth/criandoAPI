package br.com.criandoapi.security;

import br.com.criandoapi.entity.UsuarioRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-with-at-least-32-characters-long");
        jwtProperties.setExpirationMs(3_600_000);

        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void deveGerarEValidarTokenComRole() {
        String token = jwtService.generateToken("maria@email.com", UsuarioRole.ADMIN);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("maria@email.com");
        assertThat(jwtService.extractRole(token)).isEqualTo(UsuarioRole.ADMIN);
        assertThat(jwtService.isTokenValid(token, "maria@email.com")).isTrue();
    }

    @Test
    void deveInvalidarTokenComEmailDiferente() {
        String token = jwtService.generateToken("maria@email.com", UsuarioRole.USER);

        assertThat(jwtService.isTokenValid(token, "outro@email.com")).isFalse();
    }
}
