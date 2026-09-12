package com.synfonia.musicas.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotifyImportDataRequest {
    private String name;
    private String capaUrl;
    private String spotifyPlaylistId;
    private String accessToken;
    private String source;
    private List<SpotifyTrackData> tracks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpotifyTrackData {
        private String id;
        private String name;
        private String artist;
        private String album;
        private String capaUrl;
        private String uri;
    }
}
