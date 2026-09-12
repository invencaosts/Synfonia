package com.synfonia.musicas.enums;

public enum SearchAttribute {
    ALL(null),
    ARTIST("artistTerm"),
    ALBUM("albumTerm"),
    TRACK("songTerm");

    private final String itunesValue;

    SearchAttribute(String itunesValue) {
        this.itunesValue = itunesValue;
    }

    public String getItunesValue() {
        return itunesValue;
    }
}
