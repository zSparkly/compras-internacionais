package com.aula.pos.compras.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AwesomeApiResponse(@JsonProperty("USDBRL") DolarResponse dolar) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DolarResponse(String ask, @JsonProperty("create_date") String createDate) {}
}
