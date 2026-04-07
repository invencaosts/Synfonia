package com.joaopaulo.musicas.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequest {
    
    @Email
    @NotBlank
    private String email;
    
    @NotBlank
    @Size(min = 8, message = "Senha deve ter pelo menos 8 caracteres")
    private String senha;
    
    @NotBlank
    @Size(min = 8, message = "Confirmação de senha deve ter pelo menos 8 caracteres")
    private String confirmarSenha;
    
    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Size(max = 100)
    private String displayName;

    @Size(max = 100)
    private String personalName;

}