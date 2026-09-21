package com.synfonia.musicas.entities;

import com.synfonia.musicas.enums.MusicSource;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "album_ratings")
@CompoundIndex(name = "usuario_album_unico", def = "{'userId': 1, 'albumKey': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumRating {

    @Id
    private String id;

    @Indexed
    private Long userId;

    @Indexed
    private String albumKey;

    private String albumName;
    private String artista;
    private String capaUrl;
    private MusicSource source;
    private Double nota;
    private String titulo;
    private String review;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
