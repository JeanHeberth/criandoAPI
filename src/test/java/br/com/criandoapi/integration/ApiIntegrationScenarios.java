package br.com.criandoapi.integration;

import br.com.criandoapi.entity.*;
import br.com.criandoapi.exception.NegocioException;
import br.com.criandoapi.record.*;
import br.com.criandoapi.repository.*;
import br.com.criandoapi.security.*;
import br.com.criandoapi.services.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.flywaydb.core.Flyway;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
abstract class ApiIntegrationScenarios {
    static final String PREFIX = "/api/v2";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProdutoRepository produtos;
    @Autowired PedidoRepository pedidos;
    @Autowired PedidoService pedidoService;
    @Autowired JwtService jwt;
    @Autowired Flyway flyway;
    Usuario usuario;
    Usuario outro;
    Usuario admin;
    Produto produto;
    String token;
    String tokenAdmin;

    @BeforeEach
    void prepararBancoIsolado() {
        pedidos.deleteAll();
        produtos.deleteAll();
        usuarios.deleteAll();
        usuario = usuarios.save(new Usuario("Usuario Teste", "user@teste.com", PasswordUtils.hash("senha123")));
        outro = usuarios.save(new Usuario("Outro Usuario", "outro@teste.com", PasswordUtils.hash("senha123")));
        admin = new Usuario("Administrador", "admin@teste.com", PasswordUtils.hash("senha123"));
        admin.setRole(UsuarioRole.ADMIN);
        admin = usuarios.save(admin);
        produto = produtos.save(new Produto("Notebook", "Computador", new BigDecimal("10.25"), 10, ProdutoCategoria.ELETRONICO));
        token = jwt.generateToken(usuario.getEmail());
        tokenAdmin = jwt.generateToken(admin.getEmail());
    }

    private String body(int quantidade) {
        return "{\"itens\":[{\"produtoId\":" + produto.getId() + ",\"quantidade\":" + quantidade + "}]}";
    }

    private long criarPedido() throws Exception {
        MvcResult result = mvc.perform(post(PREFIX + "/pedidos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.valorTotal").value(20.50)).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cancelamentoPorPatchOuDeleteDevolveEstoqueUmaVez(boolean confirmado) throws Exception {
        long id = criarPedido();
        if (confirmado) {
            mvc.perform(patch(PREFIX + "/pedidos/" + id + "/status").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMADO\"}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(patch(PREFIX + "/pedidos/" + id + "/status").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        assertEquals(10, produtos.findById(produto.getId()).orElseThrow().getEstoque());
        mvc.perform(delete(PREFIX + "/pedidos/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().is(422)).andExpect(jsonPath("$.codigo").value("REGRA_DE_NEGOCIO"));
        assertEquals(10, produtos.findById(produto.getId()).orElseThrow().getEstoque());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deletePermitePendentesEConfirmados(boolean confirmado) throws Exception {
        long id = criarPedido();
        if (confirmado) {
            mvc.perform(patch(PREFIX + "/pedidos/" + id + "/status").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMADO\"}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(delete(PREFIX + "/pedidos/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertEquals(10, produtos.findById(produto.getId()).orElseThrow().getEstoque());
        assertEquals(PedidoStatus.CANCELADO, pedidos.findById(id).orElseThrow().getStatus());
    }

    @Test
    void idempotenciaEvitaDuplicacaoERejeitaOutroPayload() throws Exception {
        String chave = "pedido-123";
        long id = mapper.readTree(mvc.perform(post(PREFIX + "/pedidos").header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", chave).contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post(PREFIX + "/pedidos").header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", chave).contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(post(PREFIX + "/pedidos").header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", chave).contentType(MediaType.APPLICATION_JSON).content(body(3)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLITO"));
        assertEquals(1, pedidos.count());
        assertEquals(8, produtos.findById(produto.getId()).orElseThrow().getEstoque());
    }

    @Test
    void idempotenciaEhIsoladaPorUsuario() throws Exception {
        for (Usuario dono : List.of(usuario, outro)) {
            mvc.perform(post(PREFIX + "/pedidos").header("Authorization", "Bearer " + jwt.generateToken(dono.getEmail()))
                    .header("Idempotency-Key", "mesma-chave").contentType(MediaType.APPLICATION_JSON).content(body(2)))
                    .andExpect(status().isCreated());
        }
        assertEquals(2, pedidos.count());
        assertEquals(6, produtos.findById(produto.getId()).orElseThrow().getEstoque());
    }

    @Test
    void falhaEmSegundoItemReverteTodaBaixaDeEstoque() throws Exception {
        Produto semEstoque = produtos.save(new Produto("Mouse", "Mouse USB", BigDecimal.ONE, 0, ProdutoCategoria.ELETRONICO));
        String payload = "{\"itens\":[{\"produtoId\":" + produto.getId() + ",\"quantidade\":2},"
                + "{\"produtoId\":" + semEstoque.getId() + ",\"quantidade\":1}]}";
        mvc.perform(post(PREFIX + "/pedidos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().is(422));
        assertEquals(10, produtos.findById(produto.getId()).orElseThrow().getEstoque());
        assertEquals(0, pedidos.count());
    }

    @Test
    void comprasSimultaneasNaoVendemMaisQueEstoque() throws Exception {
        produto.setEstoque(1);
        produtos.save(produto);
        PedidoRequest request = new PedidoRequest(List.of(new ItemPedidoRequest(produto.getId(), 1)));
        CountDownLatch inicio = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> primeira = () -> comprarQuandoLiberado(inicio, request, usuario);
            Callable<Boolean> segunda = () -> comprarQuandoLiberado(inicio, request, outro);
            Future<Boolean> f1 = executor.submit(primeira);
            Future<Boolean> f2 = executor.submit(segunda);
            inicio.countDown();
            int sucessos = (f1.get(15, TimeUnit.SECONDS) ? 1 : 0) + (f2.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, sucessos);
        }
        assertEquals(0, produtos.findById(produto.getId()).orElseThrow().getEstoque());
        assertEquals(1, pedidos.count());
    }

    private boolean comprarQuandoLiberado(CountDownLatch inicio, PedidoRequest request, Usuario dono) throws Exception {
        assertTrue(inicio.await(5, TimeUnit.SECONDS));
        try {
            pedidoService.criar(request, UsuarioAutenticado.of(dono));
            return true;
        } catch (NegocioException ex) {
            assertTrue(ex.getMessage().contains("Estoque insuficiente"));
            return false;
        }
    }

    @Test
    void reenviosSimultaneosComMesmaChaveCriamUmPedido() throws Exception {
        PedidoRequest request = new PedidoRequest(List.of(new ItemPedidoRequest(produto.getId(), 2)));
        CountDownLatch inicio = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<PedidoResponse> criar = () -> {
                assertTrue(inicio.await(5, TimeUnit.SECONDS));
                return pedidoService.criar(request, UsuarioAutenticado.of(usuario), "concorrente");
            };
            Future<PedidoResponse> f1 = executor.submit(criar);
            Future<PedidoResponse> f2 = executor.submit(criar);
            inicio.countDown();
            assertEquals(f1.get(15, TimeUnit.SECONDS).id(), f2.get(15, TimeUnit.SECONDS).id());
        }
        assertEquals(1, pedidos.count());
        assertEquals(8, produtos.findById(produto.getId()).orElseThrow().getEstoque());
    }

    @Test
    void usuariosNaoAcessamPedidosDeTerceiros() throws Exception {
        long id = criarPedido();
        String auth = "Bearer " + jwt.generateToken(outro.getEmail());
        mvc.perform(get(PREFIX + "/pedidos/" + id).header("Authorization", auth)).andExpect(status().isNotFound());
        mvc.perform(delete(PREFIX + "/pedidos/" + id).header("Authorization", auth)).andExpect(status().isNotFound());
        mvc.perform(patch(PREFIX + "/pedidos/" + id + "/status").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELADO\"}"))
                .andExpect(status().isNotFound());
        assertEquals(8, produtos.findById(produto.getId()).orElseThrow().getEstoque());
    }

    @Test
    void permissoesProtegemProdutosEUsuarios() throws Exception {
        mvc.perform(patch(PREFIX + "/produtos/" + produto.getId() + "/estoque").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estoque\":100}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mvc.perform(patch(PREFIX + "/produtos/" + produto.getId() + "/estoque").header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estoque\":100}"))
                .andExpect(status().isOk());
        mvc.perform(get(PREFIX + "/usuarios").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get(PREFIX + "/usuarios").header("Authorization", "Bearer " + tokenAdmin)).andExpect(status().isOk());
        mvc.perform(get(PREFIX + "/usuarios/" + outro.getId()).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get(PREFIX + "/usuarios/" + usuario.getId()).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(delete(PREFIX + "/usuarios/" + outro.getId()).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "sort=senha,asc", "sort=nome,errado", "sort=nome,asc,desc", "sort=nome"})
    void paginacaoInvalidaRetorna400(String query) throws Exception {
        mvc.perform(get(PREFIX + "/produtos?" + query).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
        mvc.perform(get(PREFIX + "/pedidos?" + query).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"precoMin=-1", "precoMax=-1", "precoMin=10&precoMax=1"})
    void filtrosInvalidosRetornam400(String query) throws Exception {
        mvc.perform(get(PREFIX + "/produtos?" + query).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jwtEIdentificadorUsamMesmoContratoDeErro() throws Exception {
        for (String auth : List.of("", "Bearer invalido")) {
            MvcResult result = mvc.perform(get(PREFIX + "/produtos").header("Authorization", auth))
                    .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
                    .andExpect(jsonPath("$.requestId").isNotEmpty()).andExpect(header().exists("X-Request-ID")).andReturn();
            assertEquals(result.getResponse().getHeader("X-Request-ID"),
                    mapper.readTree(result.getResponse().getContentAsString()).get("requestId").asText());
        }
    }

    @Test
    void registroLoginEValidacaoFuncionamComPrefixoConfigurado() throws Exception {
        mvc.perform(post(PREFIX + "/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Novo Usuario\",\"email\":\"novo@teste.com\",\"senha\":\"senha123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.token").isNotEmpty());
        assertEquals(UsuarioRole.USER, usuarios.findByEmail("novo@teste.com").orElseThrow().getRole());
        mvc.perform(post(PREFIX + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"novo@teste.com\",\"senha\":\"senha123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post(PREFIX + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"novo@teste.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        mvc.perform(post(PREFIX + "/pedidos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"itens\":[null]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos", not(empty())));
    }

    @Test
    void migrationsExecutadasEManutencaoDesabilitada() throws Exception {
        assertEquals(3, flyway.info().applied().length);
        mvc.perform(delete(PREFIX + "/manutencao/banco").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
        assertEquals(3, usuarios.count());
    }

    @Test
    void documentacaoPublicaNaoExigeJwtGlobal() throws Exception {
        mvc.perform(get(PREFIX + "/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.security").doesNotExist())
                .andExpect(jsonPath("$.paths['" + PREFIX + "/pedidos'].post.parameters[?(@.name == 'Idempotency-Key')]").isNotEmpty());
    }

    @Test
    void cancelamentosSimultaneosNaoDuplicamDevolucao() throws Exception {
        long id = criarPedido();
        CountDownLatch inicio = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> cancelar = () -> {
                assertTrue(inicio.await(5, TimeUnit.SECONDS));
                try {
                    pedidoService.cancelar(id, UsuarioAutenticado.of(usuario));
                    return true;
                } catch (NegocioException ex) {
                    return false;
                }
            };
            Future<Boolean> f1 = executor.submit(cancelar);
            Future<Boolean> f2 = executor.submit(cancelar);
            inicio.countDown();
            assertEquals(1, (f1.get(15, TimeUnit.SECONDS) ? 1 : 0) + (f2.get(15, TimeUnit.SECONDS) ? 1 : 0));
        }
        assertEquals(10, produtos.findById(produto.getId()).orElseThrow().getEstoque());
    }

    @Test
    void itensRepetidosDoMesmoProdutoNaoPermitemEstoqueNegativo() throws Exception {
        String payload = "{\"itens\":[{\"produtoId\":" + produto.getId() + ",\"quantidade\":6},"
                + "{\"produtoId\":" + produto.getId() + ",\"quantidade\":6}]}";
        mvc.perform(post(PREFIX + "/pedidos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().is(422));
        assertEquals(10, produtos.findById(produto.getId()).orElseThrow().getEstoque());
        assertEquals(0, pedidos.count());
    }

    @Test
    void chavesDiferenciamMaiusculasEMinusculas() throws Exception {
        for (String chave : List.of("pedido-A", "pedido-a")) {
            mvc.perform(post(PREFIX + "/pedidos").header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", chave).contentType(MediaType.APPLICATION_JSON).content(body(2)))
                    .andExpect(status().isCreated());
        }
        assertEquals(2, pedidos.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "chave invalida", "<script>"})
    void chaveInvalidaNaoAlteraEstoque(String chave) throws Exception {
        mvc.perform(post(PREFIX + "/pedidos").header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", chave).contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isBadRequest());
        assertEquals(10, produtos.findById(produto.getId()).orElseThrow().getEstoque());
        assertEquals(0, pedidos.count());
    }

    @Test
    void precisaoMonetariaEhValidadaAntesDePersistir() throws Exception {
        mvc.perform(post(PREFIX + "/produtos").header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Novo Produto\",\"descricao\":\"Descricao\",\"preco\":1.234,\"estoque\":10,\"categoria\":\"ELETRONICO\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos[0].campo").value("preco"));
        assertEquals(1, produtos.count());
    }

    @Test
    void metricasExigemAdministrador() throws Exception {
        mvc.perform(get(PREFIX + "/actuator/metrics").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get(PREFIX + "/actuator/metrics").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.names").isArray());
    }

    private String novoUsuario(String email, String role) {
        return "{\"nome\":\"Novo Usuario\",\"email\":\"" + email + "\",\"senha\":\"senha123\""
                + (role == null ? "" : ",\"role\":\"" + role + "\"") + "}";
    }

    @Test
    void criacaoPublicaAssumeUserEBloqueiaAdminSemToken() throws Exception {
        mvc.perform(post(PREFIX + "/usuarios").contentType(MediaType.APPLICATION_JSON).content(novoUsuario("a@teste.com", null)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(post(PREFIX + "/usuarios").contentType(MediaType.APPLICATION_JSON).content(novoUsuario("b@teste.com", "ADMIN")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mvc.perform(post(PREFIX + "/usuarios").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(novoUsuario("c@teste.com", "ADMIN")))
                .andExpect(status().isForbidden());
        assertTrue(usuarios.findByEmail("b@teste.com").isEmpty());
    }

    @Test
    void adminPodeCriarUsuarioComRoleAdmin() throws Exception {
        mvc.perform(post(PREFIX + "/usuarios").header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(novoUsuario("d@teste.com", "ADMIN")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("ADMIN"));
        assertEquals(UsuarioRole.ADMIN, usuarios.findByEmail("d@teste.com").orElseThrow().getRole());
    }

    @Test
    void alteracaoDeRoleExigeAdminEProtegeUltimoAdmin() throws Exception {
        String url = PREFIX + "/usuarios/" + usuario.getId() + "/role";
        mvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(url).header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(url).header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(patch(PREFIX + "/usuarios/999999/role").header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}"))
                .andExpect(status().isNotFound());
        // dois admins: rebaixar um e permitido; rebaixar o ultimo nao
        mvc.perform(patch(url).header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch(PREFIX + "/usuarios/" + admin.getId() + "/role").header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}"))
                .andExpect(status().isConflict());
    }
}
