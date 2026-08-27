package com.fktlomas.turnos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class TurnosApplication {
    public static void main(String[] args) {
        SpringApplication.run(TurnosApplication.class, args);
    }
}
