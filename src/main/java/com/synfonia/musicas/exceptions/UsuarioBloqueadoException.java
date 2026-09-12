package com.synfonia.musicas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UsuarioBloqueadoException extends RuntimeException {
    public UsuarioBloqueadoException(String message) {
        super(message);
    }

    public UsuarioBloqueadoException() {
        super("Usuário temporariamente bloqueado devido a muitas tentativas falhas");
    }
}