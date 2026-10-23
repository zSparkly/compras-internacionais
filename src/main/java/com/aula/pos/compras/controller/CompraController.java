package com.aula.pos.compras.controller;

import com.aula.pos.compras.dto.*;
import com.aula.pos.compras.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/compras")
public class CompraController {
    private final CompraService service;

    public CompraController(CompraService service) {
        this.service = service;
    }

    @PostMapping("/simular")
    public CalculoResponse simular(@Valid @RequestBody SimulacaoRequest request) {
        return service.simular(request);
    }

    @PostMapping
    public ResponseEntity<CompraResponse> registrar(@Valid @RequestBody CompraRequest request) {
        return ResponseEntity.status(201).body(service.registrar(request));
    }
}
