package com.aula.pos.compras.controller;

import com.aula.pos.compras.dto.CompraResponse;
import com.aula.pos.compras.dto.LimiteResponse;
import com.aula.pos.compras.service.CompraService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
public class ClienteController {
    private final CompraService service;

    public ClienteController(CompraService service) {
        this.service = service;
    }

    @GetMapping("/{id}/compras")
    public List<CompraResponse> historico(@PathVariable UUID id) {
        return service.historico(id);
    }

    @GetMapping("/{id}/limite")
    public LimiteResponse limite(@PathVariable UUID id) {
        return service.limite(id);
    }
}
