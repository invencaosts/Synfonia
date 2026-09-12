package com.synfonia.musicas.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicRequest {
    private String trackName;
    private String artistName;
    private String albumName;
    private Integer limit;

    public String getCombinedTerm() {
        return Stream.of(trackName, artistName, albumName)
                .filter(field -> field != null && !field.isBlank())
                .map(String::trim)
                .collect(Collectors.joining("+"))
                .replace(" ", "+");
    }

    public boolean hasAnyParameter() {
        return (trackName != null && !trackName.isBlank()) ||
                (artistName != null && !artistName.isBlank()) ||
                (albumName != null && !albumName.isBlank());
    }

    public Integer getLimit() {
        return limit != null ? limit : 20;
    }
}