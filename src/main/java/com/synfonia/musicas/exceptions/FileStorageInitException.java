package com.synfonia.musicas.exceptions;

public class FileStorageInitException extends FileStorageException {
    public FileStorageInitException(String message) {
        super(message);
    }

    public FileStorageInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
