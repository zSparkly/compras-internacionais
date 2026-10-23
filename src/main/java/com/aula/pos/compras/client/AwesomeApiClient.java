package com.aula.pos.compras.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "awesomeApi", url = "${awesomeapi.url}")
public interface AwesomeApiClient {
    @GetMapping("/json/last/USD-BRL")
    AwesomeApiResponse consultarDolar();
}
