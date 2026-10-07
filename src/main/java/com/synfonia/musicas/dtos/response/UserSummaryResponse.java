package com.synfonia.musicas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cartão de usuário exibido na busca da comunidade. Não expõe email nem dados de conta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private Long id;
    private String username;
    private String displayName;
    private String fotoPerfil;
}
