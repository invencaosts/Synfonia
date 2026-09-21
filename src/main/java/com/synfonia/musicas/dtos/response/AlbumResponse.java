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
public class AlbumResponse {
    private String albumKey;
    private String artista;
    private String albumName;
    private String capaUrl;
    private MusicSource source;
    private String externalAlbumId;
}
