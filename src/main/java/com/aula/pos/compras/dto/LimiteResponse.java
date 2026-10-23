package com.aula.pos.compras.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record LimiteResponse(UUID clienteId, YearMonth mes, BigDecimal limiteMensal,
                             BigDecimal totalUtilizado, BigDecimal saldoDisponivel) {}
