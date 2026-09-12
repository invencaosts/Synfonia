package com.synfonia.musicas.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItunesTrackResponse {
    
    @JsonProperty("trackName")
    private String trackName;

    @JsonProperty("artistName")
    private String artistName;

    @JsonProperty("collectionName")
    private String albumName;

    @JsonProperty("releaseDate")
    private String releaseDate;

    @JsonProperty("trackId")
    private Long trackId;

    @JsonProperty("artworkUrl100")
    private String artworkUrl;

    @JsonProperty("previewUrl")
    private String previewUrl;

    public Integer getReleaseYear() {
        if (releaseDate == null || releaseDate.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}