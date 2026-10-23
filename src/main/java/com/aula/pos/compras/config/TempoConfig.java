package com.aula.pos.compras.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TempoConfig {
    @Bean
    public Clock clock(@Value("${app.zone-id}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
