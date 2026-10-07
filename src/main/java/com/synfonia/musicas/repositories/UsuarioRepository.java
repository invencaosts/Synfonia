package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<Usuario> findByEmailAndAtivoTrue(String email);

    @Query("SELECT COUNT(u) FROM Usuario u JOIN u.roles r WHERE r = :role")
    long countByRole(@Param("role") com.synfonia.musicas.enums.RoleSistema role);

    // Painel de moderação: inclui perfis privados e suspensos; termo já escapado
    @Query(value = """
            SELECT u FROM Usuario u
            WHERE (:termo = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :termo, '%')) ESCAPE '\\'
                   OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :termo, '%')) ESCAPE '\\'
                   OR (:incluirEmail = true AND LOWER(u.email) LIKE LOWER(CONCAT('%', :termo, '%')) ESCAPE '\\'))
              AND (:somenteBanidos = false OR u.banido = true)
            ORDER BY u.id DESC
            """, countQuery = """
            SELECT COUNT(u) FROM Usuario u
            WHERE (:termo = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :termo, '%')) ESCAPE '\\'
                   OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :termo, '%')) ESCAPE '\\'
                   OR (:incluirEmail = true AND LOWER(u.email) LIKE LOWER(CONCAT('%', :termo, '%')) ESCAPE '\\'))
              AND (:somenteBanidos = false OR u.banido = true)
            """)
    Page<Usuario> buscarParaModeracao(@Param("termo") String termo,
                                      @Param("incluirEmail") boolean incluirEmail,
                                      @Param("somenteBanidos") boolean somenteBanidos,
                                      Pageable pageable);

    void deleteByAtivoFalseAndDataDesativacaoBefore(java.time.LocalDateTime date);

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    // Busca da comunidade: só perfis ativos e públicos, por username ou nome de exibição.
    // O termo chega com %, _ e \\ já escapados (ver CommunityService)
    @Query(value = """
            SELECT u FROM Usuario u
            WHERE u.ativo = true
              AND (u.perfilPublico IS NULL OR u.perfilPublico = true)
              AND (u.banido IS NULL OR u.banido = false OR (u.banidoAte IS NOT NULL AND u.banidoAte < CURRENT_TIMESTAMP))
              AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :termo, '%')) ESCAPE '\\'
                   OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :termo, '%')) ESCAPE '\\')
            ORDER BY CASE WHEN LOWER(u.username) = LOWER(:termo) THEN 0
                          WHEN LOWER(u.username) LIKE LOWER(CONCAT(:termo, '%')) ESCAPE '\\' THEN 1
                          ELSE 2 END,
                     u.username ASC
            """, countQuery = """
            SELECT COUNT(u) FROM Usuario u
            WHERE u.ativo = true
              AND (u.perfilPublico IS NULL OR u.perfilPublico = true)
              AND (u.banido IS NULL OR u.banido = false OR (u.banidoAte IS NOT NULL AND u.banidoAte < CURRENT_TIMESTAMP))
              AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :termo, '%')) ESCAPE '\\'
                   OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :termo, '%')) ESCAPE '\\')
            """)
    Page<Usuario> buscarPerfisPublicos(@Param("termo") String termo, Pageable pageable);

    @Query(value = """
            SELECT u FROM Usuario u
            WHERE u.ativo = true
              AND (u.perfilPublico IS NULL OR u.perfilPublico = true)
              AND (u.banido IS NULL OR u.banido = false OR (u.banidoAte IS NOT NULL AND u.banidoAte < CURRENT_TIMESTAMP))
            ORDER BY u.dataCriacao DESC
            """, countQuery = """
            SELECT COUNT(u) FROM Usuario u
            WHERE u.ativo = true
              AND (u.perfilPublico IS NULL OR u.perfilPublico = true)
              AND (u.banido IS NULL OR u.banido = false OR (u.banidoAte IS NOT NULL AND u.banidoAte < CURRENT_TIMESTAMP))
            """)
    Page<Usuario> listarPerfisPublicosRecentes(Pageable pageable);
}