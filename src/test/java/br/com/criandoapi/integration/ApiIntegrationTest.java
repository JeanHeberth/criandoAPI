package br.com.criandoapi.integration;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:api;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "api.version-prefix=/api/v2",
        "api.manutencao.enabled=false",
        "spring.flyway.baseline-on-migrate=false",
        "JWT_SECRET=chave-de-testes-isolados-com-mais-de-trinta-e-dois-caracteres"
})
class ApiIntegrationTest extends ApiIntegrationScenarios {}
