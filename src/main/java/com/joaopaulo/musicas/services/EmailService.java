package com.joaopaulo.musicas.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    @SuppressWarnings("null")
    public void sendResetPasswordEmail(String to, String code) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        String htmlContent = "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #09090b; color: #f4f4f5; padding: 20px;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background-color: #121214; border: 1px solid #1db954; border-radius: 12px; padding: 30px;'>" +
                "<h1 style='color: #1db954; text-align: center;'>Synfonia</h1>" +
                "<p>Olá,</p>" +
                "<p>Recebemos uma solicitação para redefinir a sua senha no Synfonia.</p>" +
                "<p>Use o código abaixo no site para validar sua identidade. Este código expira em 15 minutos.</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<div style='background-color: #1db954; color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 32px; letter-spacing: 5px; display: inline-block;'>" + code + "</div>" +
                "</div>" +
                "<p style='color: #a1a1aa; font-size: 0.8em; text-align: center;'>Se você não solicitou isso, por favor ignore este e-mail.</p>" +
                "</div>" +
                "</body>" +
                "</html>";

        helper.setText(htmlContent, true);
        helper.setTo(to);
        helper.setSubject("Synfonia - Recuperação de Senha");
        helper.setFrom("contato@synfonia.me");

        mailSender.send(mimeMessage);
    }
}
