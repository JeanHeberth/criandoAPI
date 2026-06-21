package br.com.criandoapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * Verifica se a API está no ar.
     * Público — sem necessidade de token.
     * Teste básico: GET /health → 200 com body {"status":"UP"}
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "aplicacao", "criandoAPI",
                "versao", "1.0.0",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}

