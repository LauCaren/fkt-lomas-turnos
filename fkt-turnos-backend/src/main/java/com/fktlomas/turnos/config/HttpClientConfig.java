package com.fktlomas.turnos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/** Bean en vez de "new" directo en los servicios que llaman APIs externas por HTTP (ver PasswordFiltradaService), para poder mockearlo en tests. */
@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }
}
