package com.aula.pos.compras.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_compras")
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "valor_usd", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal valorUSD;
    @Column(nullable = false, precision = 30, scale = 15, updatable = false)
    private BigDecimal cotacaoUtilizada;
    @Column(name = "valor_convertido", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal valorConvertido;
    @Column(name = "taxa_servico", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal taxaServico;
    @Column(name = "tributo_simulado", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal tributoSimulado;
    @Column(name = "valor_final_brl", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal valorFinalBRL;
    @Column(nullable = false, updatable = false)
    private LocalDate dataCotacao;
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHoraCotacao;
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCompra;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    protected Compra() {}

    public Compra(Cliente cliente, BigDecimal valorUSD, BigDecimal cotacaoUtilizada,
                  BigDecimal valorConvertido, BigDecimal taxaServico, BigDecimal tributoSimulado,
                  BigDecimal valorFinalBRL, LocalDateTime dataHoraCotacao, LocalDateTime dataCompra) {
        this.cliente = cliente;
        this.valorUSD = valorUSD;
        this.cotacaoUtilizada = cotacaoUtilizada;
        this.valorConvertido = valorConvertido;
        this.taxaServico = taxaServico;
        this.tributoSimulado = tributoSimulado;
        this.valorFinalBRL = valorFinalBRL;
        this.dataCotacao = dataHoraCotacao.toLocalDate();
        this.dataHoraCotacao = dataHoraCotacao;
        this.dataCompra = dataCompra;
    }

    public UUID getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public BigDecimal getValorUSD() { return valorUSD; }
    public BigDecimal getCotacaoUtilizada() { return cotacaoUtilizada; }
    public BigDecimal getValorConvertido() { return valorConvertido; }
    public BigDecimal getTaxaServico() { return taxaServico; }
    public BigDecimal getTributoSimulado() { return tributoSimulado; }
    public BigDecimal getValorFinalBRL() { return valorFinalBRL; }
    public LocalDate getDataCotacao() { return dataCotacao; }
    public LocalDateTime getDataHoraCotacao() { return dataHoraCotacao; }
    public LocalDateTime getDataCompra() { return dataCompra; }
}
