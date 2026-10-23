package com.aula.pos.compras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ComprasApplication {
    public static void main(String[] args) {
        SpringApplication.run(ComprasApplication.class, args);
    }
}
