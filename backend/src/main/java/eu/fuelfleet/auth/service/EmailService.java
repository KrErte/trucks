package eu.fuelfleet.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@veokid.ee}")
    private String fromAddress;

    @Value("${app.base-url:https://veokid.ee}")
    private String baseUrl;

    public void sendVerificationEmail(String to, String token) {
        String link = baseUrl + "/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("Kinnita oma e-posti aadress - Veokid");
        message.setText("Tere!\n\nKinnita oma e-posti aadress, klõpsates alloleval lingil:\n\n"
                + link + "\n\nLink kehtib 24 tundi.\n\nVeokid meeskond");

        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
        }
    }

    public void sendInviteEmail(String to, String token, String companyName) {
        String link = baseUrl + "/accept-invite?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("Kutse liituda ettevõttega " + companyName + " - Veokid");
        message.setText("Tere!\n\nSind on kutsutud liituma ettevõttega " + companyName + " Veokid platvormil.\n\n"
                + "Kutse vastuvõtmiseks klõpsa alloleval lingil:\n\n"
                + link + "\n\nKutse kehtib 7 päeva.\n\nVeokid meeskond");

        try {
            mailSender.send(message);
            log.info("Invite email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send invite email to {}: {}", to, e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String to, String token) {
        String link = baseUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("Parooli taastamine - Veokid");
        message.setText("Tere!\n\nParooli taastamiseks klõpsa alloleval lingil:\n\n"
                + link + "\n\nLink kehtib 1 tund.\n\nKui sa ei soovinud parooli muuta, ignoreeri seda kirja.\n\nVeokid meeskond");

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }
}
