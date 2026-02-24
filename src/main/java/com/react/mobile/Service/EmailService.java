package com.react.mobile.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    @Value("${app.debug:false}")
    private boolean debugMode;

    public void sendVerificationEmail(String toEmail, String token, String otp) {
        String subject = "Verify your account";
        String confirmationUrl = "http://localhost:8082/api/auth/verify?token=" + token;
        String message = """
                Your OTP code is: %s
                OTP expires in 15 minutes.
                You can also verify by link: %s
                """.formatted(otp, confirmationUrl);

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(toEmail);
        email.setSubject(subject);
        email.setText(message);
        sendEmail(email, "verification");

        if (debugMode) {
            log.info("DEV OTP (email verification) for {}: {}", toEmail, otp);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String otp) {
        String subject = "Reset your password";
        String message = """
                Your password reset OTP is: %s
                OTP expires in 15 minutes.
                If you did not request this, please ignore this email.
                """.formatted(otp);

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(toEmail);
        email.setSubject(subject);
        email.setText(message);
        sendEmail(email, "password reset");

        if (debugMode) {
            log.info("DEV OTP (password reset) for {}: {}", toEmail, otp);
        }
    }

    private void sendEmail(SimpleMailMessage email, String type) {
        try {
            mailSender.send(email);
            log.info("Sent {} email to {}", type, email.getTo() != null && email.getTo().length > 0 ? email.getTo()[0] : "unknown");
        } catch (Exception e) {
            log.warn("Failed to send {} email: {}", type, e.getMessage());
        }
    }
}
