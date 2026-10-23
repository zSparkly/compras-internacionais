package com.aula.pos.compras.repository;

import com.aula.pos.compras.entity.Compra;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CompraRepository extends JpaRepository<Compra, UUID> {
    @EntityGraph(attributePaths = "cliente")
    List<Compra> findByClienteIdOrderByDataCompraDesc(UUID clienteId);

    @Query("""
            select coalesce(sum(c.valorFinalBRL), 0) from Compra c
            where c.cliente.id = :clienteId and c.dataCompra >= :inicio and c.dataCompra < :fim
            """)
    BigDecimal totalNoPeriodo(@Param("clienteId") UUID clienteId,
                             @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
