package com.aula.pos.compras;

import com.aula.pos.compras.client.*;
import com.aula.pos.compras.exception.*;
import com.aula.pos.compras.service.CotacaoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CotacaoServiceTest {
    private final AwesomeApiClient client = mock(AwesomeApiClient.class);
    private final CotacaoService service = new CotacaoService(client);

    @Test
    void converteAskEDataHoraSemPerderPrecisao() {
        when(client.consultarDolar()).thenReturn(resposta("5.4000123456789", "2026-10-16 17:00:00"));
        var result = service.consultar();
        assertEquals(new BigDecimal("5.4000123456789"), result.venda());
        assertEquals(LocalDateTime.of(2026, 10, 16, 17, 0), result.dataHora());
    }

    static Stream<Arguments> invalidas() {
        return Stream.of(
            Arguments.of(null, "2026-10-16 17:00:00"), Arguments.of("", "2026-10-16 17:00:00"),
            Arguments.of("abc", "2026-10-16 17:00:00"), Arguments.of("NaN", "2026-10-16 17:00:00"),
            Arguments.of("0", "2026-10-16 17:00:00"), Arguments.of("-5.4", "2026-10-16 17:00:00"),
            Arguments.of("5,4", "2026-10-16 17:00:00"), Arguments.of("5.4", null),
            Arguments.of("5.4", ""), Arguments.of("5.4", "2026-02-30 17:00:00"),
            Arguments.of("5.4", "2026-10-16"), Arguments.of("1E+40", "2026-10-16 17:00:00"),
            Arguments.of("0.0000000000000001", "2026-10-16 17:00:00")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidas")
    void rejeitaCotacaoOuDataInvalida(String ask, String data) {
        when(client.consultarDolar()).thenReturn(resposta(ask, data));
        assertThrows(CotacaoInvalidaException.class, service::consultar);
    }

    @Test
    void rejeitaRaizOuObjetoUsDBRLAusente() {
        when(client.consultarDolar()).thenReturn(null);
        assertThrows(CotacaoInvalidaException.class, service::consultar);
        when(client.consultarDolar()).thenReturn(new AwesomeApiResponse(null));
        assertThrows(CotacaoInvalidaException.class, service::consultar);
    }

    private AwesomeApiResponse resposta(String ask, String data) {
        return new AwesomeApiResponse(new AwesomeApiResponse.DolarResponse(ask, data));
    }
}
