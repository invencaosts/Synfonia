package com.synfonia.musicas.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "historico_reproducao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoReproducao {

    @Id
    private String id;

    @Indexed
    private Long userId;

    @Indexed
    private String trackId;

    @Indexed
    private LocalDateTime dataReproducao;

}
