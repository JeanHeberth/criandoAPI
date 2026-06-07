package br.com.criandoapi.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void deveGerarHashDiferenteDaSenhaOriginal() {
        String hash = passwordEncoder.encode("123456");

        assertThat(hash).isNotEqualTo("123456");
        assertThat(hash).startsWith("$2a$");
    }

    @Test
    void deveValidarSenhaCorreta() {
        String hash = passwordEncoder.encode("123456");

        assertThat(passwordEncoder.matches("123456", hash)).isTrue();
        assertThat(passwordEncoder.matches("errada", hash)).isFalse();
    }
}
