package br.com.criandoapi.controller;

import br.com.criandoapi.entity.PedidoStatus;
import br.com.criandoapi.exception.GlobalExceptionHandler;
import br.com.criandoapi.exception.NegocioException;
import br.com.criandoapi.record.ItemPedidoResponse;
import br.com.criandoapi.record.PedidoResponse;
import br.com.criandoapi.services.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("POST /pedidos - criar")
class PedidoControllerTest {

    private MockMvc mockMvc;
    @Mock
    private PedidoService pedidoService;

    @InjectMocks
    private PedidoController pedidoController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(pedidoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("CT01 - cria pedido com sucesso e retorna 201")
    void deveCriarPedidoComSucesso() throws Exception {
        PedidoResponse response = new PedidoResponse(
                1L,
                1L,
                "Joao Silva",
                List.of(new ItemPedidoResponse(
                        10L,
                        1L,
                        "Notebook",
                        2,
                        BigDecimal.valueOf(1000),
                        BigDecimal.valueOf(2000)
                )),
                PedidoStatus.PENDENTE,
                BigDecimal.valueOf(2000),
                LocalDateTime.now(),
                null
        );

        when(pedidoService.criar(any(), any())).thenReturn(response);

        String body = """
                {
                  "itens": [
                    { "produtoId": 1, "quantidade": 2 }
                  ]
                }
                """;

        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.itens", hasSize(1)));
    }

    @Test
    @DisplayName("CT02 - produto nao encontrado retorna 404")
    void deveRetornar404QuandoProdutoNaoExiste() throws Exception {
        when(pedidoService.criar(any(), any()))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Produto nao encontrado com id: 999"));

        String body = """
                {
                  "itens": [
                    { "produtoId": 999, "quantidade": 1 }
                  ]
                }
                """;

        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem", containsString("Produto nao encontrado")));
    }

    @Test
    @DisplayName("CT03 - estoque insuficiente retorna 422")
    void deveRetornar422QuandoEstoqueInsuficiente() throws Exception {
        when(pedidoService.criar(any(), any()))
                .thenThrow(new NegocioException("Estoque insuficiente para o produto"));

        String body = """
                {
                  "itens": [
                    { "produtoId": 1, "quantidade": 999 }
                  ]
                }
                """;

        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensagem", containsString("Estoque insuficiente")));
    }

    @Test
    @DisplayName("CT04 - payload invalido retorna 400")
    void deveRetornar400QuandoItensVazio() throws Exception {
        String body = """
                {
                  "itens": []
                }
                """;

        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos", hasSize(1)))
                .andExpect(jsonPath("$.campos[0].campo").value("itens"));
    }

    @Test
    @DisplayName("CT05 - quantidade 45035 retorna 201 quando estoque permite")
    void deveRetornar201QuandoQuantidadeMuitoAltaComEstoqueSuficiente() throws Exception {
        PedidoResponse response = new PedidoResponse(
                2L,
                1L,
                "Joao Silva",
                List.of(new ItemPedidoResponse(
                        11L,
                        1L,
                        "Notebook Gamer",
                        45035,
                        BigDecimal.valueOf(3500),
                        new BigDecimal("157622500.00")
                )),
                PedidoStatus.PENDENTE,
                new BigDecimal("157622500.00"),
                LocalDateTime.now(),
                null
        );

        when(pedidoService.criar(any(), any()))
                .thenReturn(response);

        String body = """
                {
                  "itens": [
                    { "produtoId": 1, "quantidade": 45035 }
                  ]
                }
                """;

        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.itens[0].quantidade").value(45035))
                .andExpect(jsonPath("$.valorTotal").value(157622500.00));
    }

}

