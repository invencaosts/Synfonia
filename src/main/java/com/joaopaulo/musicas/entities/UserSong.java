package com.joaopaulo.musicas.entities;

import com.joaopaulo.musicas.enums.MusicSource;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "user_songs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSong {

    @Id
    private String id;

    @Indexed
    private Long userId; // ID do usuário no PostgreSQL

    @Indexed
    private String trackId; // ID da faixa (iTunes numérico ou Spotify alfanumérico)

    @Indexed
    private MusicSource source;

    @Indexed
    private String trackName;

    @Indexed
    private String artistName;

    @Indexed
    private String albumName;

    @Indexed
    private LocalDateTime dataAdicao;

    // Campos para facilitar ordenação server-side com paginação
    @Indexed
    private String trackName;

    @Indexed
    private String artistName;

    @Indexed
    private String albumName;

}