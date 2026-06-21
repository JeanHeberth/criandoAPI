package br.com.criandoapi.exception;

import br.com.criandoapi.record.ErroResponse;
import br.com.criandoapi.record.ErroValidacaoResponse;
import br.com.criandoapi.record.ErroValidacaoResponse.CampoErro;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 400 - Erros de validação do Bean Validation (@Valid)
     * Útil para testar: campo obrigatório, tamanho, formato de email, etc.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroValidacaoResponse> handleValidacao(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<CampoErro> campos = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> new CampoErro(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ErroValidacaoResponse erro = ErroValidacaoResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                campos
        );
        return ResponseEntity.badRequest().body(erro);
    }

    /**
     * 409 - Conflito de recurso (email duplicado, produto duplicado, etc.)
     * Útil para testar: criar recurso já existente
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErroResponse> handleConflict(
            ConflictException ex, HttpServletRequest request) {

        ErroResponse erro = ErroResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflito",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    /**
     * 422 - Regra de negócio violada (ex: transição de status inválida, estoque insuficiente)
     * Útil para testar: cenários de negócio
     */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErroResponse> handleNegocio(
            NegocioException ex, HttpServletRequest request) {

        ErroResponse erro = ErroResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Erro de Negócio",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.unprocessableEntity().body(erro);
    }

    /**
     * 4xx - ResponseStatusException genérica (404 NOT_FOUND, 401 UNAUTHORIZED, etc.)
     * Útil para testar: recurso não encontrado, acesso negado
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {

        ErroResponse erro = ErroResponse.of(
                ex.getStatusCode().value(),
                ex.getStatusCode().toString(),
                ex.getReason(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(erro);
    }

    /**
     * 500 - Exceção genérica não tratada
     * Útil para testar: comportamento da API em erros inesperados
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(
            Exception ex, HttpServletRequest request) {

        ErroResponse erro = ErroResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro Interno",
                "Ocorreu um erro inesperado. Contate o suporte.",
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(erro);
    }
}

