package com.joaopaulo.musicas.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetPasswordRequest {
    @NotBlank
    @jakarta.validation.constraints.Email
    private String email;

    @NotBlank
    private String token; // Este agora será o código de 6 dígitos

    @NotBlank
    @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres")
    private String newPassword;
}
