package com.fktlomas.turnos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SecurityUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (jwtService.esValido(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtService.extraerEmail(token);
                AppUserDetails userDetails = (AppUserDetails) userDetailsService.loadUserByUsername(email);

                Instant emitidoEn = jwtService.extraerFechaEmision(token);
                // El claim "iat" del JWT se trunca a segundos (así lo exige el formato NumericDate),
                // pero passwordChangedAt se guarda con precisión de milisegundos/microsegundos. Sin
                // truncar acá, un token emitido en el mismo segundo que el alta o el cambio de
                // contraseña quedaba mal marcado como "emitido antes" y se rechazaba siempre —
                // rompía la reserva de turnos justo después de registrarse.
                Instant passwordCambiadaEn = userDetails.getUsuario().getPasswordChangedAt() == null
                        ? null : userDetails.getUsuario().getPasswordChangedAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
                if (passwordCambiadaEn != null && emitidoEn.isBefore(passwordCambiadaEn)) {
                    // Token emitido antes del último cambio de contraseña: se ignora, como si
                    // no viniera autenticado. Evita que una sesión vieja siga sirviendo hasta
                    // los 7 días de expiración si la contraseña se cambió por seguridad.
                    filterChain.doFilter(request, response);
                    return;
                }

                // Mismo mecanismo que passwordChangedAt, para "cerrar todas las sesiones" sin
                // forzar un cambio de contraseña — usado al desactivar una cuenta de staff o
                // por autoservicio desde "mi cuenta" (ver AdminUsuarioController).
                Instant sesionesRevocadasEn = userDetails.getUsuario().getSessionsRevokedAt() == null
                        ? null : userDetails.getUsuario().getSessionsRevokedAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
                if (sesionesRevocadasEn != null && emitidoEn.isBefore(sesionesRevocadasEn)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (UsernameNotFoundException e) {
                // El token es válido pero la cuenta ya no existe (ej: se borró). Seguimos
                // sin autenticar: si el endpoint es público, funciona igual; si requiere
                // auth, el usuario recibe un 401/403 normal en vez de romper el pedido.
            }
        }

        filterChain.doFilter(request, response);
    }
}
