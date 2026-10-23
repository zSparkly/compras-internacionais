package com.aula.pos.compras;

import com.aula.pos.compras.dto.*;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ValidacaoTest {
    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.001", "1000000000000"})
    void rejeitaValorForaDasRegras(String valor) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(new CompraRequest(UUID.randomUUID(), new BigDecimal(valor))).isEmpty());
            assertFalse(validator.validate(new SimulacaoRequest(new BigDecimal(valor))).isEmpty());
        }
    }

    @Test
    void exigeClienteEValorMasSimulacaoNaoExigeCliente() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertEquals(2, validator.validate(new CompraRequest(null, null)).size());
            assertEquals(1, validator.validate(new SimulacaoRequest(null)).size());
            assertTrue(validator.validate(new SimulacaoRequest(new BigDecimal("200.00"))).isEmpty());
            assertTrue(validator.validate(new CompraRequest(UUID.randomUUID(), new BigDecimal("0.01"))).isEmpty());
        }
    }

    @Test
    void nomeEEmailDoClienteSaoValidos() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertEquals(2, validator.validate(new ClienteResponse(UUID.randomUUID(), " ", "invalido")).size());
            assertTrue(validator.validate(new ClienteResponse(UUID.randomUUID(), "Ana", "ana@example.com")).isEmpty());
        }
    }
}
