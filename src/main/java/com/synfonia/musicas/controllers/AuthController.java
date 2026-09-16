package com.synfonia.musicas.controllers;

import com.synfonia.musicas.dtos.request.LoginRequest;
import com.synfonia.musicas.dtos.request.UsuarioRequest;
import com.synfonia.musicas.dtos.request.ForgotPasswordRequest;
import com.synfonia.musicas.dtos.request.ResetPasswordRequest;
import com.synfonia.musicas.dtos.response.LoginResponse;
import com.synfonia.musicas.dtos.response.UsuarioResponse;
import com.synfonia.musicas.services.AuthService;
import com.synfonia.musicas.services.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        log.info("[AuthController] Tentativa de login para: {}", request.getEmail());
        LoginResponse loginResponse = authService.login(request);

        // OPÇÃO NUCLEAR: Limpar cookies fantasmas de sessões anteriores/outros paths
        ResponseCookie deleteAccess = ResponseCookie.from("synfonia_access", "").path("/").maxAge(0).build();
        ResponseCookie deleteRefresh = ResponseCookie.from("synfonia_refresh", "").path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefresh.toString());

        // Gerar os novos cookies
        ResponseCookie accessTokenCookie = createCookie(httpRequest, "synfonia_access", loginResponse.getToken(), 24 * 60 * 60);
        ResponseCookie refreshTokenCookie = createCookie(httpRequest, "synfonia_refresh", loginResponse.getRefreshToken(), 7 * 24 * 60 * 60);

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // App nativo (Capacitor) roda em origem própria: cookie cross-site com SameSite=Lax
        // não é enviado nas requisições seguintes. Nesse caso, devolve o token no corpo
        // para o app guardar e enviar via header Authorization.
        if ("capacitor".equalsIgnoreCase(httpRequest.getHeader("X-Client-Platform"))) {
            Map<String, Object> nativeBody = new HashMap<>();
            nativeBody.put("token", loginResponse.getToken());
            nativeBody.put("refreshToken", loginResponse.getRefreshToken());
            nativeBody.put("usuario", loginResponse.getUsuario());
            return ResponseEntity.ok(nativeBody);
        }

        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Logout de usuário")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse response) {
        // Limpeza absoluta usando Path("/") e Max-Age=0
        ResponseCookie cleanAccess = createCookie(httpRequest, "synfonia_access", "", 0);
        ResponseCookie cleanRefresh = createCookie(httpRequest, "synfonia_refresh", "", 0);
        
        response.addHeader(HttpHeaders.SET_COOKIE, cleanAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cleanRefresh.toString());
        
        log.info("Logout realizado - Cookies de sessão invalidados.");
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
    public ResponseEntity<Void> verifyResetCode(@Valid @RequestBody com.synfonia.musicas.dtos.request.VerifyCodeRequest request) {
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

    @SuppressWarnings("null")
    private ResponseCookie createCookie(HttpServletRequest request, String name, String value, long maxAge) {
        String serverName = request.getServerName();
        // Cookie "Secure" só é aceito pelo navegador em HTTPS (localhost é a
        // única exceção especial). Como o frontend agora acessa a API pelo
        // proxy do Vite (mesma origem, sem CORS), SameSite=None+Secure só é
        // necessário quando a conexão real é HTTPS; em HTTP local (inclusive
        // por IP de rede), Lax sem Secure funciona e é o que o navegador aceita.
        boolean isHttps = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        String sameSite = isHttps ? "None" : "Lax";
        boolean secure = isHttps;

        log.info("[CookieService] Cookie '{}'. Server: {}, HTTPS: {}, SameSite: {}, Secure: {}",
                 name, serverName, isHttps, sameSite, secure);

        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAge)
                .sameSite(sameSite)
                .build();
    }
}