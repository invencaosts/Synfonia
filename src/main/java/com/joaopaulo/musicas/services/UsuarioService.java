package com.joaopaulo.musicas.services;

import com.joaopaulo.musicas.entities.Usuario;
import com.joaopaulo.musicas.exceptions.UnauthorizedException;
import com.joaopaulo.musicas.exceptions.UsuarioNaoEncontradoException;
import com.joaopaulo.musicas.repositories.UsuarioRepository;
import com.joaopaulo.musicas.security.UsuarioDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public Usuario updateProfile(Map<String, Object> profileData) {
        Usuario usuario = getLoggedUser();

        if (profileData.containsKey("username")) {
            String newUsername = profileData.get("username").toString();
            if (!newUsername.equals(usuario.getUsername()) && usuarioRepository.existsByUsername(newUsername)) {
                throw new com.joaopaulo.musicas.exceptions.EmailJaCadastradoException("Este username já está em uso.");
            }
            usuario.setUsername(newUsername);
        }

        if (profileData.containsKey("displayName")) usuario.setDisplayName(profileData.get("displayName").toString());
        if (profileData.containsKey("personalName")) usuario.setPersonalName(profileData.get("personalName").toString());
        if (profileData.containsKey("showPersonalName")) usuario.setShowPersonalName((Boolean) profileData.get("showPersonalName"));
        if (profileData.containsKey("showSpotifyActivity")) usuario.setShowSpotifyActivity((Boolean) profileData.get("showSpotifyActivity"));

        return usuarioRepository.save(java.util.Objects.requireNonNull(usuario));

    }


    public Usuario updateFavoriteMusic(Map<String, Object> favoriteData) {
        Usuario usuario = getLoggedUser();
        
        if (favoriteData.containsKey("favoriteTrackId")) {
            usuario.setFavoriteTrackId(favoriteData.get("favoriteTrackId").toString());
        }
        usuario.setFavoriteTrackName((String) favoriteData.get("favoriteTrackName"));
        usuario.setFavoriteTrackArtist((String) favoriteData.get("favoriteTrackArtist"));
        usuario.setFavoriteTrackCapaUrl((String) favoriteData.get("favoriteTrackCapaUrl"));
        usuario.setFavoriteTrackPreviewUrl((String) favoriteData.get("favoriteTrackPreviewUrl"));
        
        return usuarioRepository.save(java.util.Objects.requireNonNull(usuario));

    }

    public void removeFavoriteMusic() {
        Usuario usuario = getLoggedUser();
        usuario.setFavoriteTrackId(null);
        usuario.setFavoriteTrackName(null);
        usuario.setFavoriteTrackArtist(null);
        usuario.setFavoriteTrackCapaUrl(null);
        usuario.setFavoriteTrackPreviewUrl(null);
        usuarioRepository.save(java.util.Objects.requireNonNull(usuario));

    }

    public Usuario updateProfilePicture(String fotoPerfil) {
        Usuario usuario = getLoggedUser();
        usuario.setFotoPerfil(fotoPerfil);
        return usuarioRepository.save(java.util.Objects.requireNonNull(usuario));

    }

    public Usuario getMe() {
        return getLoggedUser();
    }

    private Usuario getLoggedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioDetails details)) {
            throw new UnauthorizedException("Usuário não está autenticado");
        }
        return usuarioRepository.findById(java.util.Objects.requireNonNull(details.getId()))

                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
    }
}
