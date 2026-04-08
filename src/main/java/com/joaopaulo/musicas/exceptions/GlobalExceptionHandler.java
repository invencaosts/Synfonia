package com.joaopaulo.musicas.exceptions;

import com.joaopaulo.musicas.exceptions.records.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Usuário não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErrorResponse> handleCredenciaisInvalidas(CredenciaisInvalidasException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", ex.getMessage(), request);
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErrorResponse> handleEmailJaCadastrado(EmailJaCadastradoException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Conflito de dados", ex.getMessage(), request);
    }

    @ExceptionHandler(IdorSecurityException.class)
    public ResponseEntity<ErrorResponse> handleIdorSecurity(IdorSecurityException ex, HttpServletRequest request) {
        log.warn("Tentativa de IDOR detectada: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Violação de segurança", ex.getMessage(), request);
    }

    @ExceptionHandler(UsuarioBloqueadoException.class)
    public ResponseEntity<ErrorResponse> handleUsuarioBloqueado(UsuarioBloqueadoException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.valueOf(423), "Conta bloqueada", ex.getMessage(), request);
    }

    @ExceptionHandler(SenhaInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleSenhaInvalida(SenhaInvalidaException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Senha inválida", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalMusicArgumentsException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArguments(IllegalMusicArgumentsException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Parâmetros de busca inválidos", ex.getMessage(), request);
    }

    @ExceptionHandler(MusicNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMusicNotFound(MusicNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException ex, HttpServletRequest request) {
        log.error("Falha externa: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, "Erro no provedor externo", ex.getMessage(), request);
    }

    @ExceptionHandler(SpotifyApiException.class)
    public ResponseEntity<ErrorResponse> handleSpotifyApi(SpotifyApiException ex, HttpServletRequest request) {
        log.error("Erro na API do Spotify: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, "Erro no Spotify", ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Não autorizado", ex.getMessage(), request);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageException ex, HttpServletRequest request) {
        log.error("Erro de armazenamento: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de arquivo", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return buildResponse(HttpStatus.BAD_REQUEST, "Validação falhou", 
                "Existem erros nos campos enviados", request, errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Erro crítico: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", 
                ex.getMessage() != null ? ex.getMessage() : "Erro inesperado no servidor.", request);
    }

    @SuppressWarnings("null")
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String title, String message, HttpServletRequest request) {
        return buildResponse(status, title, message, request, null);
    }

    @SuppressWarnings("null")
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String title, String message, HttpServletRequest request, Map<String, String> errors) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(
                        status.value(), 
                        title, 
                        message, 
                        LocalDateTime.now(), 
                        request.getRequestURI(), 
                        errors));
    }
}
