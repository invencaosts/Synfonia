package com.synfonia.musicas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Playlist pública vista pela comunidade. Omite dados de sincronização com o Spotify.
 * {@code tracks} só é preenchido no detalhe da playlist.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicPlaylistResponse {
    private String id;
    private Long userId;
    private String nome;
    private String vibe;
    private String capaUrl;
    private List<String> trackIds;
    private int totalMusicas;
    private List<MusicResponse> tracks;
    // Só aparece true para moderadores (para os demais a playlist bloqueada nem é listada)
    private boolean bloqueada;
    private String bloqueioMotivo;
}
