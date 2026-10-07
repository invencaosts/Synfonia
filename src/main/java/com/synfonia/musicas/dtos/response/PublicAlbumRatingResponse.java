package com.synfonia.musicas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Avaliação aberta pelo link de compartilhamento (sem login). Só dados que já seriam públicos no perfil. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicAlbumRatingResponse {
    private String id;
    private String artista;
    private String albumName;
    private String capaUrl;
    private Double nota;
    private String titulo;
    private String review;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private String username;
    private String displayName;
    private String fotoPerfil;
}
