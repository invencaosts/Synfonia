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

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;
    
    @Email
    @NotBlank
    private String email;
    
    @Email
    @NotBlank
    private String confirmarEmail;
    
    @NotBlank
    @Size(min = 8, message = "Senha deve ter pelo menos 8 caracteres")
    private String senha;
    
    @NotBlank
    @Size(min = 8, message = "Confirmação de senha deve ter pelo menos 8 caracteres")
    private String confirmarSenha;
    
    @NotBlank
    @Size(min = 3, max = 50, message = "O Nome de Exibição deve ter entre 3 e 50 caracteres")
    private String displayName;
    
    // Opcionais para atualização posterior
    private String personalName;
    private Boolean showPersonalName;
}