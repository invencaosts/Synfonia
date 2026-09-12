package com.synfonia.musicas.entities;

import com.synfonia.musicas.dtos.response.ItunesTrackResponse;
import com.synfonia.musicas.enums.MusicSource;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "musicas") // Define explicitamente a coleção no Mongo
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicEntity {

    @Id
    private String id;

    @Indexed
    private String nome;

    @Indexed
    private String artista;
    private String album;
    private Integer anoLancamento;
    private String previewUrl;
    private String capaUrl;
    private String uri;
    private MusicSource source;

    public MusicEntity(ItunesTrackResponse dto, String trackId) {
        this.id = trackId;
        this.nome = dto.getTrackName();
        this.artista = dto.getArtistName();
        this.album = dto.getAlbumName();
        this.capaUrl = dto.getArtworkUrl();
        this.previewUrl = dto.getPreviewUrl();
        this.anoLancamento = dto.getReleaseYear();
        this.source = MusicSource.ITUNES;
    }
}