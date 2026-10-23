package com.aula.pos.compras.service;

import com.aula.pos.compras.client.AwesomeApiClient;
import com.aula.pos.compras.client.AwesomeApiResponse;
import com.aula.pos.compras.exception.CotacaoInvalidaException;
import com.aula.pos.compras.exception.ServicoCotacaoException;
import feign.FeignException;
import feign.codec.DecodeException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import org.springframework.stereotype.Service;

@Service
public class CotacaoService {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private final AwesomeApiClient client;

    public CotacaoService(AwesomeApiClient client) {
        this.client = client;
    }

    public Cotacao consultar() {
        AwesomeApiResponse response;
        try {
            response = client.consultarDolar();
        } catch (DecodeException e) {
            throw new CotacaoInvalidaException();
        } catch (FeignException e) {
            throw new ServicoCotacaoException(e.status() == 408 || e.status() == 504 || isTimeout(e));
        }
        if (response == null || response.dolar() == null
                || response.dolar().ask() == null || response.dolar().createDate() == null) {
            throw new CotacaoInvalidaException();
        }
        try {
            BigDecimal venda = new BigDecimal(response.dolar().ask()).stripTrailingZeros();
            LocalDateTime dataHora = LocalDateTime.parse(response.dolar().createDate(), FORMATO);
            // Preserve the exact rate within the DECIMAL(30,15) column; never silently truncate it.
            if (venda.signum() <= 0 || venda.scale() > 15 || (long) venda.precision() - venda.scale() > 15) {
                throw new CotacaoInvalidaException();
            }
            return new Cotacao(venda, dataHora);
        } catch (NumberFormatException | DateTimeParseException | ArithmeticException e) {
            throw new CotacaoInvalidaException();
        }
    }

    private boolean isTimeout(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
