package com.aula.pos.compras.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ClienteNaoEncontradoException extends NegocioException {
    public ClienteNaoEncontradoException(UUID id) {
        super(HttpStatus.NOT_FOUND, "CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado: " + id);
    }
}
