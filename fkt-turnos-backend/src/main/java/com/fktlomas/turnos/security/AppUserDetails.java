package com.fktlomas.turnos.security;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class AppUserDetails implements UserDetails {

    // Mantenido en sync a mano con AuthService.ROLES_MFA_OBLIGATORIO: los tres roles de
    // oficina/finanzas (ven caja, historia clínica o ambas) no son opcionales para 2FA.
    private static final Set<Role> ROLES_MFA_OBLIGATORIO = EnumSet.of(Role.ADMIN, Role.SECRETARIA, Role.CONTADOR);

    private final Usuario usuario;

    public AppUserDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ADMIN/SECRETARIA escriben sobre historia clínica y caja física, y CONTADOR ve todos
        // los movimientos de caja del centro — para estos tres el 2FA no es opcional. Mientras
        // no lo tengan activado no reciben su rol real (así que /api/admin/**, /api/contador/**
        // les devuelve 403, ver SecurityConfig), solo el authority "pendiente" de abajo, que
        // igual alcanza para entrar a /api/mi-cuenta/mfa/** (no restringido por rol) y
        // activarlo — en cuanto lo activan, el siguiente request ya recalcula esto desde la
        // base y les da el rol completo, sin necesidad de un login nuevo.
        if (ROLES_MFA_OBLIGATORIO.contains(usuario.getRole()) && !usuario.isMfaHabilitado()) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name() + "_MFA_PENDIENTE"));
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
