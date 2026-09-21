package com.synfonia.musicas.dtos.response;

import com.synfonia.musicas.enums.MusicSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumRatingResponse {
    private String id;
    private String albumKey;
    private String artista;
    private String albumName;
    private String capaUrl;
    private MusicSource source;
    private Double nota;
    private String titulo;
    private String review;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
