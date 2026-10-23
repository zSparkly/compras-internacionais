package com.aula.pos.compras.exception;

import org.springframework.http.HttpStatus;

public class NegocioException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;

    public NegocioException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }
}
