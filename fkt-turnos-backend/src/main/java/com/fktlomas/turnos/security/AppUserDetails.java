package com.fktlomas.turnos.security;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserDetails implements UserDetails {

    private final Usuario usuario;

    public AppUserDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // El CONTADOR ve todos los movimientos de caja del centro — a diferencia de
        // médico/admin, para este rol el 2FA no es opcional. Mientras no lo tenga activado
        // no recibe ROLE_CONTADOR (así que /api/contador/** le devuelve 403, ver
        // SecurityConfig), solo el authority genérico de abajo, que igual le alcanza para
        // entrar a /api/mi-cuenta/mfa/** (no restringido por rol) y activarlo — en cuanto lo
        // activa, el siguiente request ya recalcula esto desde la base y le da el rol
        // completo, sin necesidad de un login nuevo.
        if (usuario.getRole() == Role.CONTADOR && !usuario.isMfaHabilitado()) {
            return List.of(new SimpleGrantedAuthority("ROLE_CONTADOR_MFA_PENDIENTE"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }
}
