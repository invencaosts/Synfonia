package com.synfonia.musicas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PublicRatingShareResponse {
    private String token;
    private String url;
}
