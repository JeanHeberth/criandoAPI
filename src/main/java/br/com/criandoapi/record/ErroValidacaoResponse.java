package br.com.criandoapi.record;

import java.time.LocalDateTime;
import java.util.List;

public record ErroValidacaoResponse(
        int status,
        String erro,
        String mensagem,
        String path,
        LocalDateTime timestamp,
        List<CampoErro> campos
) {
    public record CampoErro(String campo, String mensagem) {}

    public static ErroValidacaoResponse of(int status, String path, List<CampoErro> campos) {
        return new ErroValidacaoResponse(
                status,
                "Erro de Validação",
                "Um ou mais campos estão inválidos",
                path,
                LocalDateTime.now(),
                campos
        );
    }
}

