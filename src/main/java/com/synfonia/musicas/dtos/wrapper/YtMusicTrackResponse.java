package com.synfonia.musicas.dtos.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YtMusicTrackResponse {
    private String id;
    private String nome;
    private String artista;
    private String album;
    private String capaUrl;
    private String previewUrl;
    private String uri;
    private String source;
}
