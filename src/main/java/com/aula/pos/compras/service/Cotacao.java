package com.aula.pos.compras.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Cotacao(BigDecimal venda, LocalDateTime dataHora) {}
