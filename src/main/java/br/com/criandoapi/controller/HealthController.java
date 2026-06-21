package br.com.criandoapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Verifica a disponibilidade da API")
public class HealthController {

    @GetMapping
    @Operation(
            summary = "Verifica status da API",
            description = "Endpoint público que retorna o status de saúde da aplicação. Não requer autenticação."
    )
    @ApiResponse(
            responseCode = "200",
            description = "API está operacional",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\"status\":\"UP\",\"aplicacao\":\"criandoAPI\",\"versao\":\"1.0.0\",\"timestamp\":\"2026-06-21T17:15:16.414411\"}")
            )
    )
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "aplicacao", "criandoAPI",
                "versao", "1.0.0",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}

