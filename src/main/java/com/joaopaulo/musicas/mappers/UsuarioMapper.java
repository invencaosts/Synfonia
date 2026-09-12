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

    @org.mapstruct.Mapping(target = "socialLinks", expression = "java(buildSocialLinks(entity))")
    UsuarioResponse toResponse(Usuario entity);

    @Named("buildSocialLinks")
    default java.util.Map<String, String> buildSocialLinks(Usuario entity) {
        java.util.Map<String, String> links = new java.util.HashMap<>();
        if (entity.getInstagramLink() != null) links.put("instagram", entity.getInstagramLink());
        if (entity.getSpotifyLink() != null) links.put("spotify", entity.getSpotifyLink());
        if (entity.getYoutubeLink() != null) links.put("youtube", entity.getYoutubeLink());
        return links;
    }

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