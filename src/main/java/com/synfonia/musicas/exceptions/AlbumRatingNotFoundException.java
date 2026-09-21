package com.synfonia.musicas.exceptions;

public class AlbumRatingNotFoundException extends RuntimeException {
    public AlbumRatingNotFoundException(String message) {
        super(message);
    }
    public AlbumRatingNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
