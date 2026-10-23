package com.aula.pos.compras.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponse(LocalDateTime timestamp, int status, String codigo,
                           String mensagem, List<ErroCampo> detalhes) {
    public record ErroCampo(String campo, String mensagem) {}
}
