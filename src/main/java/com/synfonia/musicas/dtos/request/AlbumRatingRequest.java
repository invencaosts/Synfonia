package com.synfonia.musicas.dtos.request;

import com.synfonia.musicas.enums.MusicSource;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumRatingRequest {

    @NotNull
    private MusicSource source;

    @NotBlank
    private String artista;

    @NotBlank
    private String albumName;

    private String capaUrl;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private Double nota;

    @Size(max = 100, message = "O título deve ter no máximo 100 caracteres")
    private String titulo;

    @Size(max = 2000, message = "A resenha deve ter no máximo 2000 caracteres")
    private String review;
}
