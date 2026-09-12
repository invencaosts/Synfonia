package com.synfonia.musicas.dtos.request;

import com.synfonia.musicas.enums.MusicSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicSaveRequest {
    private String trackId;
    private String nome;
    private String artista;
    private String album;
    private String capaUrl;
    private String previewUrl;
    private String uri;
    private Integer anoLancamento;
    private MusicSource source;
}
