package com.aula.pos.compras.exception;

import org.springframework.http.HttpStatus;

public class ServicoCotacaoException extends NegocioException {
    public ServicoCotacaoException(boolean timeout) {
        super(timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE,
                timeout ? "COTACAO_TIMEOUT" : "COTACAO_INDISPONIVEL",
                timeout ? "Tempo limite excedido ao consultar a cotação"
                        : "Serviço de cotações temporariamente indisponível");
    }
}
