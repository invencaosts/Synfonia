package com.joaopaulo.musicas.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    @JsonIgnore
    private String token;
    
    @JsonIgnore
    private String refreshToken;
    
    private UsuarioResponse usuario;
}