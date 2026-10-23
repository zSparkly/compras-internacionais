package com.aula.pos.compras.service;

import com.aula.pos.compras.dto.*;
import com.aula.pos.compras.entity.Cliente;
import com.aula.pos.compras.entity.Compra;
import com.aula.pos.compras.exception.ClienteNaoEncontradoException;
import com.aula.pos.compras.exception.LimiteMensalExcedidoException;
import com.aula.pos.compras.repository.ClienteRepository;
import com.aula.pos.compras.repository.CompraRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompraService {
    private static final BigDecimal LIMITE_MENSAL = new BigDecimal("10000.00");
    private final ClienteRepository clientes;
    private final CompraRepository compras;
    private final CotacaoService cotacoes;
    private final CalculoService calculos;
    private final Clock clock;

    public CompraService(ClienteRepository clientes, CompraRepository compras,
                         CotacaoService cotacoes, CalculoService calculos, Clock clock) {
        this.clientes = clientes;
        this.compras = compras;
        this.cotacoes = cotacoes;
        this.calculos = calculos;
        this.clock = clock;
    }

    public CalculoResponse simular(SimulacaoRequest request) {
        return calculos.calcular(request.valorUSD(), cotacoes.consultar());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CompraResponse registrar(CompraRequest request) {
        // Serialize purchases for the same customer until commit, including concurrent requests.
        Cliente cliente = clientes.findByIdForUpdate(request.clienteId())
                .orElseThrow(() -> new ClienteNaoEncontradoException(request.clienteId()));
        CalculoResponse calculo = calculos.calcular(request.valorUSD(), cotacoes.consultar());
        LocalDateTime agora = LocalDateTime.now(clock);
        BigDecimal utilizado = totalMensal(cliente.getId(), YearMonth.from(agora));
        if (utilizado.add(calculo.valorFinalBRL()).compareTo(LIMITE_MENSAL) > 0) {
            throw new LimiteMensalExcedidoException();
        }
        Compra compra = new Compra(cliente, calculo.valorUSD(), calculo.cotacaoUtilizada(),
                calculo.valorConvertido(), calculo.taxaServico(), calculo.tributoSimulado(),
                calculo.valorFinalBRL(), calculo.dataHoraCotacao(), agora);
        return toResponse(compras.saveAndFlush(compra));
    }

    public List<CompraResponse> historico(UUID clienteId) {
        verificarCliente(clienteId);
        return compras.findByClienteIdOrderByDataCompraDesc(clienteId).stream()
                .map(this::toResponse).toList();
    }

    public LimiteResponse limite(UUID clienteId) {
        verificarCliente(clienteId);
        YearMonth mes = YearMonth.now(clock);
        BigDecimal utilizado = totalMensal(clienteId, mes).setScale(2);
        return new LimiteResponse(clienteId, mes, LIMITE_MENSAL, utilizado,
                LIMITE_MENSAL.subtract(utilizado));
    }

    private BigDecimal totalMensal(UUID clienteId, YearMonth mes) {
        return compras.totalNoPeriodo(clienteId, mes.atDay(1).atStartOfDay(),
                mes.plusMonths(1).atDay(1).atStartOfDay());
    }

    private void verificarCliente(UUID id) {
        if (!clientes.existsById(id)) {
            throw new ClienteNaoEncontradoException(id);
        }
    }

    private CompraResponse toResponse(Compra compra) {
        Cliente cliente = compra.getCliente();
        return new CompraResponse(compra.getId(),
                new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getEmail()),
                compra.getValorUSD(), compra.getCotacaoUtilizada(), compra.getValorConvertido(),
                compra.getTaxaServico(), compra.getTributoSimulado(), compra.getValorFinalBRL(),
                compra.getDataCotacao(), compra.getDataHoraCotacao(), compra.getDataCompra());
    }
}
