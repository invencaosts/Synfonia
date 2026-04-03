package com.joaopaulo.musicas.controllers;

import com.joaopaulo.musicas.dtos.request.LoginRequest;
import com.joaopaulo.musicas.dtos.request.UsuarioRequest;
import com.joaopaulo.musicas.dtos.request.ForgotPasswordRequest;
import com.joaopaulo.musicas.dtos.request.ResetPasswordRequest;
import com.joaopaulo.musicas.dtos.response.LoginResponse;
import com.joaopaulo.musicas.dtos.response.UsuarioResponse;
import com.joaopaulo.musicas.services.AuthService;
import com.joaopaulo.musicas.services.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para registro e login de usuários")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "Registro de novo usuário")
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login de usuário")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(request);
        
        // Criar Cookies HttpOnly para Access Token e Refresh Token
        ResponseCookie accessTokenCookie = createCookie(httpRequest, "accessToken", loginResponse.getToken(), 24 * 60 * 60); // 1 dia
        ResponseCookie refreshTokenCookie = createCookie(httpRequest, "refreshToken", loginResponse.getRefreshToken(), 7 * 24 * 60 * 60); // 7 dias
        
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        
        // O corpo da resposta agora contém APENAS o usuário (tokens são @JsonIgnore)
        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Logout de usuário")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse response) {
        ResponseCookie cleanAccess = createCookie(httpRequest, "accessToken", "", 0);
        ResponseCookie cleanRefresh = createCookie(httpRequest, "refreshToken", "", 0);
        
        response.addHeader(HttpHeaders.SET_COOKIE, cleanAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cleanRefresh.toString());
        
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Solicitar recuperação de senha")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.createPasswordResetToken(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Verificar código de recuperação")
    @PostMapping("/verify-reset-code")
    public ResponseEntity<Void> verifyResetCode(@Valid @RequestBody com.joaopaulo.musicas.dtos.request.VerifyCodeRequest request) {
        passwordResetService.verifyResetCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Redefinir senha com código")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getEmail(), request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Obter dados do usuário logado (Validação de Sessão)")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> getMe() {
        return ResponseEntity.ok(authService.getAuthenticatedUser());
    }

    private ResponseCookie createCookie(HttpServletRequest request, String name, String value, long maxAge) {
        // Determina se deve usar Secure (apenas HTTPS)
        // Ativa se for HTTPS ou se estiver vindo de um proxy reverso seguro (como ngrok ou cloudflare)
        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        
        // Em localhost (desenvolvimento), permitimos HTTP comum para facilitar testes
        if (request.getServerName().equals("localhost") || request.getServerName().equals("127.0.0.1")) {
            isSecure = false;
        }

        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(isSecure) 
                .path("/")
                .maxAge(maxAge)
                .sameSite("Strict") // Mais seguro que Lax, impede CSRF de forma mais agressiva
                .build();
    }
}