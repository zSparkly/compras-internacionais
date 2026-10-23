package com.aula.pos.compras.exception;

import com.aula.pos.compras.dto.ErroResponse;
import com.aula.pos.compras.dto.ErroResponse.ErroCampo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErroResponse> negocio(NegocioException e) {
        return resposta(e.getStatus(), e.getCodigo(), e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e) {
        List<ErroCampo> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage())).toList();
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO", "Dados de entrada inválidos", detalhes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonInvalido() {
        return resposta(HttpStatus.BAD_REQUEST, "JSON_INVALIDO",
                "JSON inválido, tipo incorreto ou campo não permitido no corpo da requisição", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido() {
        return resposta(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", "Parâmetro inválido", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente() {
        return resposta(HttpStatus.NOT_FOUND, "ROTA_NAO_ENCONTRADA", "Rota não encontrada", List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoInvalido() {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_PERMITIDO",
                "Método HTTP não permitido", List.of());
    }

    @ExceptionHandler(CannotAcquireLockException.class)
    public ResponseEntity<ErroResponse> ocupada() {
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, "OPERACAO_OCUPADA",
                "Outra compra está em processamento. Tente novamente", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception e) {
        LOG.error("Falha inesperada ao processar a requisição", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Não foi possível processar a solicitação", List.of());
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String codigo,
                                                 String mensagem, List<ErroCampo> detalhes) {
        return ResponseEntity.status(status).body(new ErroResponse(LocalDateTime.now(clock),
                status.value(), codigo, mensagem, detalhes));
    }
}
