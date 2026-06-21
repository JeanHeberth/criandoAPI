package br.com.criandoapi.record;

import java.time.LocalDateTime;

public record ErroResponse(
        int status,
        String erro,
        String mensagem,
        String path,
        LocalDateTime timestamp
) {
    public static ErroResponse of(int status, String erro, String mensagem, String path) {
        return new ErroResponse(status, erro, mensagem, path, LocalDateTime.now());
    }
}

