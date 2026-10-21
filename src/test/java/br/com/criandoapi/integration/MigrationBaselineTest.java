package br.com.criandoapi.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.*;

class MigrationBaselineTest {
    @Test
    void adotaSchemaLegadoSemExcluirDados() {
        DriverManagerDataSource banco = new DriverManagerDataSource(
                "jdbc:h2:mem:legado;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__schema_inicial.sql")).execute(banco);
        JdbcTemplate jdbc = new JdbcTemplate(banco);
        jdbc.update("INSERT INTO usuarios(nome,email,senha,role) VALUES ('Legado','legado@teste.com','hash','USER')");

        Flyway flyway = Flyway.configure().dataSource(banco).baselineOnMigrate(true)
                .baselineVersion("1").locations("classpath:db/migration").load();
        assertEquals(3, flyway.migrate().migrationsExecuted);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE email='legado@teste.com'", Integer.class));
        jdbc.queryForList("SELECT idempotency_key, idempotency_key_hash, request_hash FROM pedidos");
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }
}
