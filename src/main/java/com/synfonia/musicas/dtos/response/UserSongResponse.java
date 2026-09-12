package com.synfonia.musicas.dtos.response;

import java.time.LocalDateTime;

public record UserSongResponse(
        String id,
        Long userId,
        MusicResponse music,
        LocalDateTime dataAdicao
) {}
