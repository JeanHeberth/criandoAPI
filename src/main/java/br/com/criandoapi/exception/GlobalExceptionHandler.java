package br.com.criandoapi.exception;

import br.com.criandoapi.record.ErroResponse;
import br.com.criandoapi.record.ErroValidacaoResponse;
import br.com.criandoapi.record.ErroValidacaoResponse.CampoErro;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({org.springframework.dao.ConcurrencyFailureException.class,
            jakarta.persistence.LockTimeoutException.class, jakarta.persistence.PessimisticLockException.class})
    public ResponseEntity<ErroResponse> handleConcorrencia(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErroResponse.of(409, "Conflito concorrente",
                "Recurso em uso por outra operacao. Tente novamente.", request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> handleParametroAusente(
            org.springframework.web.bind.MissingServletRequestParameterException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErroResponse.of(400, "Requisicao invalida",
                "Parametro obrigatorio ausente: " + ex.getParameterName(), request.getRequestURI()));
    }

    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,
            org.springframework.web.servlet.NoHandlerFoundException.class})
    public ResponseEntity<ErroResponse> handleNaoEncontrado(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(404).body(ErroResponse.of(404, "Recurso nao encontrado",
                "Endpoint nao encontrado", request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> handleMetodo(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return ResponseEntity.status(405).header("Allow", String.join(", ", ex.getSupportedMethods() == null
                ? new String[0] : ex.getSupportedMethods())).body(ErroResponse.of(405, "Metodo nao permitido",
                "Metodo HTTP nao permitido para este endpoint", request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> handleContentType(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(415).body(ErroResponse.of(415, "Tipo de conteudo invalido",
                "Content-Type nao suportado", request.getRequestURI()));
    }


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
     * 400 - JSON malformado ou valor inválido para tipo/campo (ex.: enum inválido no body)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleMensagemNaoLegivel(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String mensagem = "O corpo da requisicao esta mal formado ou contem valor invalido.";
        Throwable causa = ex.getMostSpecificCause();
        if (causa instanceof InvalidFormatException ife) {
            String valor = String.valueOf(ife.getValue());
            Class<?> tipoAlvo = ife.getTargetType();

            if (tipoAlvo != null && tipoAlvo.isEnum()) {
                String aceitos = acceptedEnumValues(tipoAlvo);
                mensagem = "Valor '" + valor + "' invalido. Valores aceitos para este campo: " + aceitos + ".";
            } else {
                mensagem = "Valor '" + valor + "' invalido para o tipo esperado no campo informado.";
            }
        }

        ErroResponse erro = ErroResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Requisicao Invalida",
                mensagem,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(erro);
    }

    /**
     * 400 - Parametro de rota/query invalido (ex.: /produtos/categoria/TESTE)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String mensagem = "Parametro '" + ex.getName() + "' invalido.";
        Class<?> tipo = ex.getRequiredType();
        if (tipo != null && tipo.isEnum()) {
            mensagem = "Valor '" + ex.getValue() + "' invalido para o parametro '" + ex.getName()
                    + "'. Valores aceitos: " + acceptedEnumValues(tipo) + ".";
        }

        ErroResponse erro = ErroResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Requisicao Invalida",
                mensagem,
                request.getRequestURI()
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
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(erro);
    }

    /**
     * 422 - Erros de integridade/dados (ex: valor fora do limite decimal da coluna)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErroResponse.of(409,
                "Conflito de dados", "Operacao conflita com um recurso existente ou vinculado.", request.getRequestURI()));
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

        log.error("Erro inesperado: {}", ex.getClass().getName());
        ErroResponse erro = ErroResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro Interno",
                "Ocorreu um erro inesperado. Contate o suporte.",
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(erro);
    }

    private String acceptedEnumValues(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }
}

