package com.synfonia.musicas.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistRequest {
    private String nome;
    private String vibe;
    private boolean publico;
    private String capaUrl;
}
