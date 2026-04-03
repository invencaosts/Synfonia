package com.joaopaulo.musicas.services;

import com.joaopaulo.musicas.entities.Usuario;
import com.joaopaulo.musicas.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final UsuarioRepository usuarioRepository;

    // Executa todo dia às 03:00 da manhã
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteInactiveUsers() {
        log.info("Iniciando rotina de limpeza de contas desativadas...");
        
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(7);
        usuarioRepository.deleteByAtivoFalseAndDataDesativacaoBefore(thresholdDate);
        log.info("Limpeza de contas desativadas concluída.");
    }
}
