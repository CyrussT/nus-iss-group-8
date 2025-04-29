package com.group8.rbs.service.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmailServiceFactory {

    private final Map<String, EmailService> emailServices;

    @Autowired
    public EmailServiceFactory(Map<String, EmailService> emailServices) {
        this.emailServices = emailServices;
    }

    public EmailService getEmailService(String type) {
        return emailServices.getOrDefault(type, emailServices.get("customEmailService"));
    }
}
