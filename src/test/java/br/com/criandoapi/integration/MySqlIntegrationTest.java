package br.com.criandoapi.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("mysql")
@Testcontainers
@SpringBootTest(properties = {
        "spring.config.import=",
        "api.version-prefix=/api/v2",
        "api.manutencao.enabled=false",
        "spring.flyway.baseline-on-migrate=false",
        "JWT_SECRET=chave-de-testes-isolados-com-mais-de-trinta-e-dois-caracteres"
})
class MySqlIntegrationTest extends ApiIntegrationScenarios {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36");

    @DynamicPropertySource
    static void bancoIsolado(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", mysql::getJdbcUrl);
        properties.add("spring.datasource.username", mysql::getUsername);
        properties.add("spring.datasource.password", mysql::getPassword);
    }
}
