package com.aula.pos.compras.service;

import com.aula.pos.compras.dto.CalculoResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class CalculoService {
    private static final BigDecimal CEM_DOLARES = new BigDecimal("100");
    private static final BigDecimal TAXA_MENOR = new BigDecimal("0.05");
    private static final BigDecimal TAXA_MAIOR = new BigDecimal("0.02");
    private static final BigDecimal TRIBUTO = new BigDecimal("0.10");

    public CalculoResponse calcular(BigDecimal valorUSD, Cotacao cotacao) {
        BigDecimal convertido = moeda(valorUSD.multiply(cotacao.venda()));
        BigDecimal percentual = valorUSD.compareTo(CEM_DOLARES) < 0 ? TAXA_MENOR : TAXA_MAIOR;
        BigDecimal taxa = moeda(convertido.multiply(percentual));
        BigDecimal tributo = moeda(convertido.multiply(TRIBUTO));
        BigDecimal total = convertido.add(taxa).add(tributo);
        return new CalculoResponse(moeda(valorUSD), cotacao.venda(), convertido, taxa, tributo,
                total, cotacao.dataHora().toLocalDate(), cotacao.dataHora());
    }

    private BigDecimal moeda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
