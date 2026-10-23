package com.aula.pos.compras.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CalculoResponse(
        BigDecimal valorUSD, BigDecimal cotacaoUtilizada, BigDecimal valorConvertido,
        BigDecimal taxaServico, BigDecimal tributoSimulado, BigDecimal valorFinalBRL,
        LocalDate dataCotacao, LocalDateTime dataHoraCotacao
) {}
