package com.synfonia.musicas.entities;

import com.synfonia.musicas.dtos.response.ItunesTrackResponse;
import com.synfonia.musicas.enums.MusicSource;
import jakarta.persistence.*;
import lombok.*;

/**
 * Catálogo local de faixas (cache das fontes externas). O id é o id da faixa na fonte
 * (iTunes numérico, YouTube/Spotify alfanumérico), atribuído pela aplicação.
 */
@Entity
@Table(name = "musicas", indexes = {
        @Index(name = "idx_musicas_nome", columnList = "nome"),
        @Index(name = "idx_musicas_artista", columnList = "artista")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicEntity {

    @Id
    @Column(length = 128)
    private String id;

    @Column(columnDefinition = "TEXT")
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String artista;

    @Column(columnDefinition = "TEXT")
    private String album;

    private Integer anoLancamento;

    @Column(columnDefinition = "TEXT")
    private String previewUrl;

    @Column(columnDefinition = "TEXT")
    private String capaUrl;

    @Column(columnDefinition = "TEXT")
    private String uri;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
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
