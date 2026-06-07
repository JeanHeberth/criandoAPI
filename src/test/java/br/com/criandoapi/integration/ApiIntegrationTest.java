package br.com.criandoapi.integration;

import br.com.criandoapi.entity.Categoria;
import br.com.criandoapi.entity.Produto;
import br.com.criandoapi.entity.Usuario;
import br.com.criandoapi.entity.UsuarioRole;
import br.com.criandoapi.repository.CategoriaRepository;
import br.com.criandoapi.repository.ProdutoRepository;
import br.com.criandoapi.repository.UsuarioRepository;
import br.com.criandoapi.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @BeforeEach
    void limparDados() {
        produtoRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveRegistrarELogarUsuario() throws Exception {
        String registroBody = objectMapper.writeValueAsString(Map.of(
                "nome", "Maria",
                "email", "maria@email.com",
                "senha", "123456"
        ));

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value("maria@email.com"));

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "email", "maria@email.com",
                "senha", "123456"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void deveBloquearRotaProtegidaSemToken() throws Exception {
        mockMvc.perform(get("/produtos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token ausente"));
    }

    @Test
    void deveNegarCriacaoDeCategoriaParaUsuarioComum() throws Exception {
        String token = salvarUsuarioEToken("user@email.com", UsuarioRole.USER);

        String body = objectMapper.writeValueAsString(Map.of(
                "nome", "Eletronicos",
                "descricao", "Itens eletronicos"
        ));

        mockMvc.perform(post("/categorias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Acesso negado"));
    }

    @Test
    void devePermitirAdminCriarCategoriaEProduto() throws Exception {
        String token = salvarUsuarioEToken("admin@email.com", UsuarioRole.ADMIN);

        String categoriaBody = objectMapper.writeValueAsString(Map.of(
                "nome", "Eletronicos",
                "descricao", "Itens eletronicos"
        ));

        String categoriaResponse = mockMvc.perform(post("/categorias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoriaBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Eletronicos"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long categoriaId = objectMapper.readTree(categoriaResponse).get("id").asLong();

        String produtoBody = objectMapper.writeValueAsString(Map.of(
                "nome", "Teclado",
                "descricao", "Mecanico",
                "preco", 250.00,
                "estoque", 10,
                "categoriaId", categoriaId
        ));

        mockMvc.perform(post("/produtos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaNome").value("Eletronicos"));
    }

    @Test
    void deveImpedirExclusaoDeCategoriaComProdutos() throws Exception {
        String token = salvarUsuarioEToken("admin@email.com", UsuarioRole.ADMIN);

        Categoria categoria = categoriaRepository.save(new Categoria("Eletronicos", "Desc"));
        produtoRepository.save(new Produto("Teclado", "Desc", new BigDecimal("100.00"), 5, categoria));

        mockMvc.perform(delete("/categorias/" + categoria.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Categoria possui produtos vinculados"));
    }

    @Test
    void deveListarProdutosComPaginacao() throws Exception {
        String token = salvarUsuarioEToken("user@email.com", UsuarioRole.USER);

        Categoria categoria = categoriaRepository.save(new Categoria("Casa", "Utilidades"));
        produtoRepository.save(new Produto("Panela", "Grande", new BigDecimal("89.90"), 3, categoria));

        mockMvc.perform(get("/produtos")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].categoriaNome").value("Casa"));
    }

    private String salvarUsuarioEToken(String email, UsuarioRole role) {
        Usuario usuario = new Usuario("Teste", email, passwordEncoder.encode("123456"));
        usuario.setRole(role);
        usuarioRepository.save(usuario);
        return jwtService.generateToken(email, role);
    }
}
