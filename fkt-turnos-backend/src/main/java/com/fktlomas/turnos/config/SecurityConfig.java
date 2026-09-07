package com.fktlomas.turnos.config;

import com.fktlomas.turnos.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // API stateless sin cookies de sesión (el JWT va en el header Authorization), así
                // que no hay nada que un CSRF pueda explotar — pero sí sirve endurecer las respuestas
                // igual contra otros vectores (XSS reflejado en el Swagger UI, clickjacking, sniffing
                // de content-type) con cabeceras que Spring Security no manda por defecto (CSP, HSTS).
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                // La única página HTML que sirve este backend es Swagger UI (springdoc).
                                // Verificado cargándola de verdad en el navegador: script-src ya no
                                // necesita 'unsafe-inline' (el bundle estático de swagger-ui-5.x no
                                // inyecta ningún <script> inline). style-src sí lo sigue necesitando —
                                // probado primero con un hash puntual (sha256-RL3ie0nH+...), pero
                                // swagger-ui-bundle.js fija estilos vía el atributo style="..." en el DOM
                                // dinámicamente, no vía <style>: los hash-source de CSP solo cubren
                                // elementos <style>/<script>, no atributos style inline, así que ningún
                                // hash fijo alcanza. Bajo riesgo real: es el bundle de un tercero
                                // conocido, sirviendo solo la documentación de la API, no contenido de
                                // usuario.
                                "default-src 'self'; "
                                        + "script-src 'self'; "
                                        + "style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; "
                                        + "frame-ancestors 'none'; "
                                        + "object-src 'none'; "
                                        + "base-uri 'none'"
                        ))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/especialidades/**", "/api/medicos/**").permitAll()
                        // Reseñas aprobadas del centro entero (opiniones.html) — las de un médico puntual
                        // ya están cubiertas arriba, bajo /api/medicos/**.
                        .requestMatchers(HttpMethod.GET, "/api/resenas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/turnos/confirmacion").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contacto", "/api/postulaciones").permitAll()
                        // El estado lo consulta el wizard de reserva sin loguearse necesariamente, y al
                        // webhook le pega Mercado Pago, no un usuario con sesión — la seguridad de este
                        // último es la validación de firma (ver MercadoPagoService.validarFirma), no el JWT.
                        .requestMatchers(HttpMethod.GET, "/api/pagos/mercadopago/estado").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pagos/mercadopago/webhook").permitAll()
                        // "/v3/api-docs" (sin barra final, el JSON del OpenAPI en sí) va aparte de
                        // "/v3/api-docs/**" para que quede explícito que hace falta cubrir ambos.
                        .requestMatchers("/docs/**", "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Puerta gruesa por URL: ambos roles de staff de oficina entran acá. La
                        // distinción fina (qué puede tocar cada uno dentro de /api/admin/**) se
                        // resuelve con @PreAuthorize a nivel de método/clase en cada controller
                        // — ver AdminMedicoController, AdminCajaController, etc. Se eligió reusar
                        // este namespace en vez de duplicarlo en /api/secretaria/** para no
                        // duplicar también los ~7 componentes de frontend que ya pegan acá.
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SECRETARIA")
                        .requestMatchers("/api/medico/**").hasRole("MEDICO")
                        .requestMatchers("/api/contador/**").hasRole("CONTADOR")
                        .anyRequest().authenticated()
                )
                // Sin esto, Spring Security responde 403 tanto para "no autenticado" (falta el
                // token, o es inválido/expiró/quedó viejo tras un cambio de contraseña) como para
                // "autenticado pero sin el rol necesario". El frontend solo puede distinguir un
                // 401 de un 403, así que sin este entry point no había forma de que, ante una
                // sesión vencida, cerrara sesión y mandara a un paciente o admin de vuelta al
                // login — se quedaba con la pantalla rota mostrando errores genéricos.
                .exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("""
                            {"timestamp":"%s","status":401,"mensaje":"No autenticado o la sesión venció"}
                            """.formatted(Instant.now()));
                }))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
