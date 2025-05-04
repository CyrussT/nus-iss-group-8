package com.group8.rbs.service.email;

import com.group8.rbs.util.EmailWrapper;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;

@Service("resendEmailService")
public class ResendEmailService implements EmailService {

    private final EmailWrapper emailWrapper;

    public ResendEmailService(EmailWrapper emailWrapper) {
        this.emailWrapper = emailWrapper;
    }

    @Override
    public boolean sendEmail(String toEmail, String subject, String body) throws MessagingException {
        return emailWrapper.sendEmail(toEmail, subject, body);
    }
}
