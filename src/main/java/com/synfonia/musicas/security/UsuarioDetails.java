package com.synfonia.musicas.security;

import com.synfonia.musicas.entities.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String senhaHash;
    private final boolean ativo;
    private final boolean banido;
    private final int nivel;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Authorities = "ROLE_<papel>" para cada papel + o código de cada permissão (ex.: "USUARIOS_BANIR").
     * Papéis e permissões são definidos em código (RoleSistema/Permissao); o banco só guarda quais papéis o usuário tem.
     */
    public UsuarioDetails(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenha();
        this.ativo = usuario.isAtivo();
        this.banido = usuario.isBanidoAgora();
        this.nivel = usuario.getNivelMaximo();

        List<GrantedAuthority> lista = new ArrayList<>();
        usuario.getNomesRoles().forEach(nome -> lista.add(new SimpleGrantedAuthority("ROLE_" + nome)));
        usuario.getCodigosPermissoes().forEach(codigo -> lista.add(new SimpleGrantedAuthority(codigo)));
        this.authorities = List.copyOf(lista);
    }

    public int getNivel() {
        return nivel;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !banido;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
