package com.aula.pos.compras;

import com.aula.pos.compras.service.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class CalculoServiceTest {
    private final CalculoService service = new CalculoService();

    @ParameterizedTest
    @CsvSource({
        "200.00,5.40,1080.00,21.60,108.00,1209.60",
        "99.99,5.40,539.95,27.00,54.00,620.95",
        "100.00,5.40,540.00,10.80,54.00,604.80",
        "0.01,5.40,0.05,0.00,0.01,0.06",
        "1.00,5.405,5.41,0.27,0.54,6.22",
        "100.01,1.00,100.01,2.00,10.00,112.01"
    })
    void calculaComBigDecimalEArredondamentoConsistente(String usd, String ask, String convertido,
                                                       String taxa, String tributo, String total) {
        var data = LocalDateTime.of(2026, 10, 16, 17, 0);
        var result = service.calcular(new BigDecimal(usd), new Cotacao(new BigDecimal(ask), data));
        assertEquals(new BigDecimal(convertido), result.valorConvertido());
        assertEquals(new BigDecimal(taxa), result.taxaServico());
        assertEquals(new BigDecimal(tributo), result.tributoSimulado());
        assertEquals(new BigDecimal(total), result.valorFinalBRL());
        assertEquals(data, result.dataHoraCotacao());
        assertEquals(data.toLocalDate(), result.dataCotacao());
    }
}
