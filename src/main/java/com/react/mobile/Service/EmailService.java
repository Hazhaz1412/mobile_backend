package com.react.mobile.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Account verification";
        String confirmationUrl = "http://localhost:8082/api/auth/verify?token=" + token;
        String message = "Click the link to activate your account: " + confirmationUrl;

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(toEmail);
        email.setSubject(subject);
        email.setText(message);

        try {
            mailSender.send(email);
            System.out.println("Mail sent to " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send verification email to " + toEmail + ": " + e.getMessage());
            // Don't throw - allow registration to proceed even if mail fails
        }
    }
}