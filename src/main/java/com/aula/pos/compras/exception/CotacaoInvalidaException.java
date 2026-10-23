package com.aula.pos.compras.exception;

import org.springframework.http.HttpStatus;

public class CotacaoInvalidaException extends NegocioException {
    public CotacaoInvalidaException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "COTACAO_INVALIDA",
                "O serviço de cotações retornou uma cotação inválida");
    }
}
