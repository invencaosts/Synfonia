package com.synfonia.musicas.mappers;

import com.synfonia.musicas.dtos.request.UsuarioRequest;
import com.synfonia.musicas.dtos.response.UsuarioResponse;
import com.synfonia.musicas.entities.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @org.mapstruct.Mapping(target = "personalName", defaultValue = "")
    Usuario toEntity(UsuarioRequest request);

    @org.mapstruct.Mapping(target = "socialLinks", expression = "java(buildSocialLinks(entity))")
    @org.mapstruct.Mapping(target = "roles", expression = "java(new java.util.ArrayList<>(entity.getNomesRoles()))")
    @org.mapstruct.Mapping(target = "permissoes", expression = "java(new java.util.ArrayList<>(entity.getCodigosPermissoes()))")
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
                .perfilPublico(usuario.getPerfilPublico() != null ? usuario.getPerfilPublico() : true)
                .showCurtidas(usuario.getShowCurtidas() != null ? usuario.getShowCurtidas() : true)
                .showAvaliacoes(usuario.getShowAvaliacoes() != null ? usuario.getShowAvaliacoes() : true)
                .dataDesativacao(usuario.getDataDesativacao())
                .roles(new java.util.ArrayList<>(usuario.getNomesRoles()))
                .permissoes(new java.util.ArrayList<>(usuario.getCodigosPermissoes()))
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