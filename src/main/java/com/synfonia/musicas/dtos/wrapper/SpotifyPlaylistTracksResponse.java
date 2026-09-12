package com.synfonia.musicas.dtos.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyPlaylistTracksResponse {
    private List<Item> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private Track track;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Track {
        private String id;
        private String name;
        private List<Artist> artists;
        private Album album;
        private String uri;
        @JsonProperty("preview_url")
        private String previewUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Artist {
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Album {
        private String name;
        private List<Image> images;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        private String url;
    }
}
