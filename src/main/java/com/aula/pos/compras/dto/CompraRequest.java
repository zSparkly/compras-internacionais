package com.aula.pos.compras.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CompraRequest(
        @NotNull(message = "Cliente é obrigatório") UUID clienteId,
        @NotNull(message = "Valor em USD é obrigatório")
        @Positive(message = "Valor em USD deve ser maior que zero")
        @Digits(integer = 12, fraction = 2, message = "Valor em USD deve ter até 12 inteiros e 2 casas decimais")
        BigDecimal valorUSD
) {}
