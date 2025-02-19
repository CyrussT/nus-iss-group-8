package com.group8.rbs.util;

import org.springframework.beans.factory.annotation.Value;

import com.resend.*;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

public class EmailWrapper {

    @Value("${email.apikey}")
    private String resendKey;

    public boolean sendEmail(String to, String subject, String body) {
        Resend resend = new Resend(resendKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
            .from("RBS <rbs@resend.dev>")
            .to(to)
            .subject(subject)
            .html(body)
            .build();

        try {
            CreateEmailResponse data = resend.emails().send(params);
            System.out.println(data.getId());
            
        } catch (ResendException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
