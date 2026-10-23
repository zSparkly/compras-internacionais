package com.aula.pos.compras.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ClienteResponse(UUID id, @NotBlank String nome, @NotBlank @Email String email) {}
