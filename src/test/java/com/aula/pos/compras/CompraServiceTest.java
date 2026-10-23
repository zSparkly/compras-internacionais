package com.aula.pos.compras;

import com.aula.pos.compras.dto.*;
import com.aula.pos.compras.entity.*;
import com.aula.pos.compras.exception.*;
import com.aula.pos.compras.repository.*;
import com.aula.pos.compras.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CompraServiceTest {
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final CompraRepository compras = mock(CompraRepository.class);
    private final CotacaoService cotacoes = mock(CotacaoService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T02:59:59Z"), ZoneId.of("America/Sao_Paulo"));
    private final CompraService service = new CompraService(clientes, compras, cotacoes, new CalculoService(), clock);
    private final UUID id = UUID.randomUUID();

    private void preparar(String total) {
        Cliente cliente = mock(Cliente.class);
        when(cliente.getId()).thenReturn(id);
        when(clientes.findByIdForUpdate(id)).thenReturn(Optional.of(cliente));
        when(cotacoes.consultar()).thenReturn(new Cotacao(new BigDecimal("5.40"), LocalDateTime.of(2026, 9, 30, 17, 0)));
        when(compras.totalNoPeriodo(eq(id), any(), any())).thenReturn(new BigDecimal(total));
        when(compras.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void aceitaExatamenteDezMilEPersisteOsValores() {
        preparar("8790.40");
        var compra = service.registrar(new CompraRequest(id, new BigDecimal("200")));
        assertEquals(new BigDecimal("1209.60"), compra.valorFinalBRL());
        assertEquals(LocalDateTime.of(2026, 9, 30, 23, 59, 59), compra.dataCompra());
        verify(compras).totalNoPeriodo(id, LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0));
        verify(compras).saveAndFlush(any());
    }

    @Test
    void rejeitaUmCentavoAcimaDoLimiteSemSalvar() {
        preparar("8790.41");
        assertThrows(LimiteMensalExcedidoException.class,
                () -> service.registrar(new CompraRequest(id, new BigDecimal("200"))));
        verify(compras, never()).saveAndFlush(any());
    }

    @Test
    void clienteInexistenteNaoConsultaCotacaoNemSalva() {
        assertThrows(ClienteNaoEncontradoException.class,
                () -> service.registrar(new CompraRequest(id, BigDecimal.ONE)));
        verifyNoInteractions(cotacoes, compras);
    }

    @Test
    void falhaExternaNaoSalvaCompra() {
        preparar("0");
        when(cotacoes.consultar()).thenThrow(new ServicoCotacaoException(false));
        assertThrows(ServicoCotacaoException.class,
                () -> service.registrar(new CompraRequest(id, BigDecimal.ONE)));
        verify(compras, never()).saveAndFlush(any());
    }

    @Test
    void simulacaoNaoConsultaNemGravaNoBanco() {
        when(cotacoes.consultar()).thenReturn(new Cotacao(new BigDecimal("5.40"), LocalDateTime.now(clock)));
        assertEquals(new BigDecimal("1209.60"), service.simular(new SimulacaoRequest(new BigDecimal("200"))).valorFinalBRL());
        verifyNoInteractions(clientes, compras);
    }

    @Test
    void consultaLimiteDoMesNoFusoConfigurado() {
        when(clientes.existsById(id)).thenReturn(true);
        when(compras.totalNoPeriodo(eq(id), any(), any())).thenReturn(new BigDecimal("1209.60"));
        var result = service.limite(id);
        assertEquals(YearMonth.of(2026, 9), result.mes());
        assertEquals(new BigDecimal("8790.40"), result.saldoDisponivel());
    }

    @Test
    void historicoNaoBuscaCotacaoAtual() {
        when(clientes.existsById(id)).thenReturn(true);
        when(compras.findByClienteIdOrderByDataCompraDesc(id)).thenReturn(List.of());
        assertTrue(service.historico(id).isEmpty());
        verifyNoInteractions(cotacoes);
    }
}
