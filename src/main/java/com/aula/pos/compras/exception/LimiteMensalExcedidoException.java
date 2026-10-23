package com.aula.pos.compras.exception;

import org.springframework.http.HttpStatus;

public class LimiteMensalExcedidoException extends NegocioException {
    public LimiteMensalExcedidoException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "LIMITE_MENSAL_EXCEDIDO",
                "A compra excede o limite mensal de R$ 10.000,00");
    }
}
