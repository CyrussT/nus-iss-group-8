package com.group8.rbs.service.email;

import jakarta.mail.MessagingException;

public interface EmailService {
    boolean sendEmail(String toEmail, String subject, String body) throws MessagingException;
}
