package br.com.criandoapi.integration;

import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.entity.UsuarioRole;
import br.com.criandoapi.repository.UsuarioRepository;
import br.com.criandoapi.security.JwtService;
import br.com.criandoapi.security.PasswordUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:manutencao;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "api.version-prefix=/v1",
        "api.manutencao.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "JWT_SECRET=chave-de-testes-isolados-com-mais-de-trinta-e-dois-caracteres"
})
class ManutencaoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;

    @Test
    void limpezaExigeAdministradorMesmoQuandoHabilitada() throws Exception {
        Usuario usuario = usuarios.save(new Usuario("Usuario", "user@teste.com", PasswordUtils.hash("senha123")));
        Usuario admin = new Usuario("Admin", "admin@teste.com", PasswordUtils.hash("senha123"));
        admin.setRole(UsuarioRole.ADMIN);
        admin = usuarios.save(admin);
        mvc.perform(delete("/v1/manutencao/banco").header("Authorization", "Bearer " + jwt.generateToken(usuario.getEmail())))
                .andExpect(status().isForbidden());
        assertEquals(2, usuarios.count());
        mvc.perform(delete("/v1/manutencao/banco").header("Authorization", "Bearer " + jwt.generateToken(admin.getEmail())))
                .andExpect(status().isNoContent());
        assertEquals(0, usuarios.count());
    }
}
