package br.com.criandoapi.controller;

import br.com.criandoapi.services.ManutencaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.version-prefix}/manutencao")
@Tag(name = "Manutenção", description = "Endpoints de apoio para automacao de testes (uso local/estudo)")
public class ManutencaoController {

    private final ManutencaoService manutencaoService;

    public ManutencaoController(ManutencaoService manutencaoService) {
        this.manutencaoService = manutencaoService;
    }

    @DeleteMapping("/banco")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Limpa o banco de dados",
            description = "Remove TODOS os pedidos, produtos e usuarios (nessa ordem, respeitando as FKs). " +
                    "Uso exclusivo para zerar o estado do banco entre execucoes de automacao de testes locais " +
                    "— nao usar em ambiente compartilhado. CT base: sucesso (204), sem token (401)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "204 No Content - Banco limpo"),
            @ApiResponse(responseCode = "401", description = "401 Unauthorized - Token ausente/invalido")
    })
    public ResponseEntity<Void> limparBanco() {
        manutencaoService.limparBanco();
        return ResponseEntity.noContent().build();
    }
}
