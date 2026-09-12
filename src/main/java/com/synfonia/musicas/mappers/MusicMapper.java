package com.synfonia.musicas.mappers;

import com.synfonia.musicas.dtos.response.ItunesTrackResponse;
import com.synfonia.musicas.dtos.response.MusicResponse;
import com.synfonia.musicas.entities.MusicEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring") // O componentModel = "spring" já é aplicado via build.gradle
public interface MusicMapper {

    @Mapping(target = "id", source = "trackId")
    @Mapping(target = "nome", source = "trackName")
    @Mapping(target = "artista", source = "artistName")
    @Mapping(target = "album", source = "albumName")
    @Mapping(target = "capaUrl", source = "artworkUrl")
    @Mapping(target = "previewUrl", source = "previewUrl")
    @Mapping(target = "anoLancamento", expression = "java(dto.getReleaseYear())")
    @Mapping(target = "source", constant = "ITUNES")
    MusicEntity toEntity(ItunesTrackResponse dto);

    @Mapping(target = "previewUrl", source = "previewUrl")
    MusicResponse toResponse(MusicEntity entity);

    List<MusicResponse> toResponseList(List<MusicEntity> entities);
}