package com.synfonia.musicas.dtos.response;

import com.synfonia.musicas.enums.MusicSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicResponse {
    private String id;
    private String nome;
    private String artista;
    private String album;
    private String capaUrl;
    private Integer anoLancamento;
    private String previewUrl;
    private String uri;
    private MusicSource source;

    public boolean isSpotify() {
        return source == MusicSource.SPOTIFY;
    }
}