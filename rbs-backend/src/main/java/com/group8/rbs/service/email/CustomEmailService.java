package com.group8.rbs.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;

@Service
public class CustomEmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public CustomEmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public boolean sendEmail(String toEmail, String subject, String body) throws MessagingException {
        try {
            var mimeMessage = javaMailSender.createMimeMessage();
            var messageHelper = new MimeMessageHelper(mimeMessage, true); // 'true' for multipart (HTML)

            messageHelper.setFrom(fromEmail);
            messageHelper.setTo(toEmail);
            messageHelper.setSubject(subject);
            messageHelper.setText(body, true); // Email body (HTML format)

            javaMailSender.send(mimeMessage);
            System.out.println("Email sent successfully to " + toEmail);
            return true; // Successfully sent
        } catch (MailException e) {
            e.printStackTrace();
            return false; // Failed to send email
        }
    }
}
