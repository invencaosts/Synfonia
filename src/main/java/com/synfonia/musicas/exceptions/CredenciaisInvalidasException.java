package com.synfonia.musicas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException(String message) {
        super(message);
    }

    public CredenciaisInvalidasException() {
        super("Credenciais inválidas (email ou senha incorretos)");
    }
}