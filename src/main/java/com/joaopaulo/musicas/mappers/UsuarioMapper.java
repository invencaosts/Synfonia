package com.joaopaulo.musicas.mappers;

import com.joaopaulo.musicas.dtos.request.UsuarioRequest;
import com.joaopaulo.musicas.dtos.response.UsuarioResponse;
import com.joaopaulo.musicas.entities.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @org.mapstruct.Mapping(target = "personalName", defaultValue = "")
    Usuario toEntity(UsuarioRequest request);

    UsuarioResponse toResponse(Usuario entity);

    @Named("usuarioToUsuarioResponse")
    default UsuarioResponse usuarioToUsuarioResponse(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .username(usuario.getUsername())
                .displayName(usuario.getDisplayName())
                .personalName(usuario.getPersonalName())
                .showPersonalName(usuario.getShowPersonalName() != null ? usuario.getShowPersonalName() : true)
                .showSpotifyActivity(usuario.getShowSpotifyActivity() != null ? usuario.getShowSpotifyActivity() : true)
                .dataDesativacao(usuario.getDataDesativacao())
                .papel(usuario.getPapel())
                .ativo(usuario.isAtivo())
                .dataCriacao(usuario.getDataCriacao())
                .ultimoLogin(usuario.getUltimoLogin())
                .favoriteTrackId(usuario.getFavoriteTrackId())
                .favoriteTrackName(usuario.getFavoriteTrackName())
                .favoriteTrackArtist(usuario.getFavoriteTrackArtist())
                .favoriteTrackCapaUrl(usuario.getFavoriteTrackCapaUrl())
                .favoriteTrackPreviewUrl(usuario.getFavoriteTrackPreviewUrl())
                .fotoPerfil(usuario.getFotoPerfil())
                .usernameChanged(usuario.isUsernameChanged())
                .build();

    }
}