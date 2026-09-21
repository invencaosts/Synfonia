package com.synfonia.musicas.util;

import com.synfonia.musicas.enums.MusicSource;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class AlbumKeyUtil {

    private static final Pattern DIACRITICOS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private AlbumKeyUtil() {
    }

    public static String gerarChave(MusicSource source, String artista, String albumName) {
        return source + "|" + normalizar(artista) + "|" + normalizar(albumName);
    }

    private static String normalizar(String valor) {
        if (valor == null) return "";
        String semAcento = DIACRITICOS.matcher(Normalizer.normalize(valor, Normalizer.Form.NFD)).replaceAll("");
        return ESPACOS.matcher(semAcento.trim().toLowerCase()).replaceAll(" ");
    }
}
