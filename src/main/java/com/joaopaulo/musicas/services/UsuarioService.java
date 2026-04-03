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
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public Usuario updateFavoriteMusic(Map<String, Object> favoriteData) {
        Usuario usuario = getLoggedUser();
        
        if (favoriteData.containsKey("favoriteTrackId")) {
            usuario.setFavoriteTrackId(Long.valueOf(favoriteData.get("favoriteTrackId").toString()));
        }
        usuario.setFavoriteTrackName((String) favoriteData.get("favoriteTrackName"));
        usuario.setFavoriteTrackArtist((String) favoriteData.get("favoriteTrackArtist"));
        usuario.setFavoriteTrackCapaUrl((String) favoriteData.get("favoriteTrackCapaUrl"));
        usuario.setFavoriteTrackPreviewUrl((String) favoriteData.get("favoriteTrackPreviewUrl"));
        
        return usuarioRepository.save(usuario);
    }

    public void removeFavoriteMusic() {
        Usuario usuario = getLoggedUser();
        usuario.setFavoriteTrackId(null);
        usuario.setFavoriteTrackName(null);
        usuario.setFavoriteTrackArtist(null);
        usuario.setFavoriteTrackCapaUrl(null);
        usuario.setFavoriteTrackPreviewUrl(null);
        usuarioRepository.save(usuario);
    }

    public Usuario updateProfile(Map<String, Object> profileData) {
        Usuario usuario = getLoggedUser();
        
        if (profileData.containsKey("displayName")) {
            String nick = (String) profileData.get("displayName");
            // Validação: Sem emojis (simplificado: checagem de intervalo unicode se necessário, 
            // mas aqui faremos uma checagem básica de sanidade)
            if (containsEmoji(nick)) {
                throw new IllegalArgumentException("Emojis não são permitidos no Nome de Exibição");
            }
            usuario.setDisplayName(nick);
        }
        
        if (profileData.containsKey("personalName")) {
            String pName = (String) profileData.get("personalName");
            if (pName != null && !pName.matches("^[A-Za-zÀ-ÖØ-öø-ÿ\\s]*$")) {
                throw new IllegalArgumentException("Nome Pessoal deve conter apenas letras e espaços");
            }
            usuario.setPersonalName(pName);
        }
        
        if (profileData.containsKey("showPersonalName")) {
            usuario.setShowPersonalName((Boolean) profileData.get("showPersonalName"));
        }
        
        if (profileData.containsKey("showSpotifyActivity")) {
            usuario.setShowSpotifyActivity((Boolean) profileData.get("showSpotifyActivity"));
        }
        
        return usuarioRepository.save(usuario);
    }

    public void deactivateAccount() {
        Usuario usuario = getLoggedUser();
        if (!usuario.isAtivo()) {
            throw new IllegalStateException("Conta já está desativada");
        }
        usuario.setAtivo(false);
        usuario.setDataDesativacao(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    private boolean containsEmoji(String str) {
        if (str == null) return false;
        return str.codePoints().anyMatch(cp -> 
            (cp >= 0x1F600 && cp <= 0x1F64F) || // Emoticons
            (cp >= 0x1F300 && cp <= 0x1F5FF) || // Misc Symbols and Pictographs
            (cp >= 0x1F680 && cp <= 0x1F6FF) || // Transport and Map
            (cp >= 0x2600 && cp <= 0x26FF)   || // Misc Symbols
            (cp >= 0x2700 && cp <= 0x27BF)   || // Dingbats
            (cp >= 0xFE00 && cp <= 0xFE0F)   || // Variation Selectors
            (cp >= 0x1F900 && cp <= 0x1F9FF)    // Supplemental Symbols and Pictographs
        );
    }

    public Usuario updateProfilePicture(String fotoPerfil) {
        Usuario usuario = getLoggedUser();
        usuario.setFotoPerfil(fotoPerfil);
        return usuarioRepository.save(usuario);
    }

    public Usuario getMe() {
        return getLoggedUser();
    }

    private Usuario getLoggedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioDetails details)) {
            throw new UnauthorizedException("Usuário não está autenticado");
        }
        return usuarioRepository.findById(details.getId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
    }
}
