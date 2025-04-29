package com.group8.rbs.service.email;

import java.util.Map;

public class AdminApprovedEmailContent implements EmailContentStrategy {
    @Override
    public String buildSubject(Map<String, Object> params) {
        return "RBS Booking Approved";
    }

    @Override
    public String buildBody(Map<String, Object> params) {
        return "<html><body>" +
                "<p>Dear Student,</p>" +
                "<p>Your booking has been approved.</p>" +
                "<p><strong>Booking ID:</strong> " + params.get("bookingId") + "</p>" +
                "<p>Best regards,<br>Resource Booking System</p>" +
                "</body></html>";
    }
}
