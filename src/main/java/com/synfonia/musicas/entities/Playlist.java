package com.synfonia.musicas.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "playlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Playlist {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String nome;
    
    private String vibe;
    
    private boolean publico;

    private String capaUrl;

    private boolean syncSpotify;

    private String spotifyPlaylistId;

    @Builder.Default
    private List<String> trackIds = new ArrayList<>();
}
