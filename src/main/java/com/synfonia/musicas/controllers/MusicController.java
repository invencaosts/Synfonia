package com.synfonia.musicas.controllers;

import com.synfonia.musicas.dtos.response.AlbumResponse;
import com.synfonia.musicas.dtos.response.MusicResponse;
import com.synfonia.musicas.enums.MusicSource;
import com.synfonia.musicas.mappers.MusicMapper;
import com.synfonia.musicas.services.MusicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/musicas")
@RequiredArgsConstructor
@Tag(name = "Músicas", description = "Endpoints para busca e gerenciamento do catálogo musical")
public class MusicController {

    private final MusicService musicService;
    private final MusicMapper musicMapper;

    @Operation(summary = "Busca músicas com filtros opcionais",
            description = "O nome é obrigatório. Artista e Álbum ajudam a refinar a busca na Apple Music.")
    @GetMapping("/search")
    public ResponseEntity<List<MusicResponse>> search(
            @RequestParam("nome") String nome,
            @RequestParam(value = "artista", required = false) String artista,
            @RequestParam(value = "album", required = false) String album,
            @RequestParam(value = "tipo", defaultValue = "all") String tipo,
            @RequestParam(value = "source", defaultValue = "ITUNES") MusicSource source) {

        var entities = musicService.searchByFilter(nome, artista, album, tipo, 100, source);
        return ResponseEntity.ok(musicMapper.toResponseList(entities));
    }

    @Operation(summary = "Busca álbuns (agrupa faixas encontradas pelo nome do álbum/artista)",
            description = "Cada álbum é identificado por uma chave derivada de fonte+artista+nome do álbum, pois as fontes externas não expõem um ID de álbum estável.")
    @GetMapping("/albuns/busca")
    public ResponseEntity<List<AlbumResponse>> searchAlbums(
            @RequestParam("nome") String nome,
            @RequestParam(value = "source", defaultValue = "ITUNES") MusicSource source) {

        return ResponseEntity.ok(musicService.searchAlbums(nome, source));
    }

    @Operation(summary = "Busca a tracklist completa de um álbum",
            description = "Para YouTube Music usa o browseId (externalAlbumId) do álbum; para iTunes resolve por artista+nome do álbum.")
    @GetMapping("/albuns/faixas")
    public ResponseEntity<List<MusicResponse>> albumTracks(
            @RequestParam(value = "externalAlbumId", required = false) String externalAlbumId,
            @RequestParam("artista") String artista,
            @RequestParam("album") String album,
            @RequestParam(value = "source", defaultValue = "ITUNES") MusicSource source) {

        var entities = musicService.buscarFaixasDoAlbum(externalAlbumId, artista, album, source);
        return ResponseEntity.ok(musicMapper.toResponseList(entities));
    }

    @Operation(summary = "Proxy de imagem externa (capa de álbum)",
            description = "Evita bloqueio de CORS ao capturar a imagem de compartilhamento (html2canvas) e problemas de auth em <img> no app nativo.")
    @GetMapping("/proxy-imagem")
    public ResponseEntity<byte[]> proxyImagem(@RequestParam("url") String url) {
        return musicService.proxyImagem(url);
    }

    @Operation(summary = "Busca uma música específica pelo ID interno")
    @GetMapping("/{id}")
    public ResponseEntity<MusicResponse> getById(@PathVariable("id") String id) {
        var entity = musicService.findById(id);
        return ResponseEntity.ok(musicMapper.toResponse(entity));
    }

    @Operation(summary = "Busca múltiplas músicas pelos IDs internos (Batch)")
    @PostMapping("/batch")
    public ResponseEntity<List<MusicResponse>> getByIds(@RequestBody List<String> ids) {
        var entities = musicService.findAllByIds(ids);
        return ResponseEntity.ok(musicMapper.toResponseList(entities));
    }
}
