package com.synfonia.musicas.dtos.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.synfonia.musicas.dtos.response.ItunesTrackResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItunesSearchWrapper {
    private List<ItunesTrackResponse> results;
}