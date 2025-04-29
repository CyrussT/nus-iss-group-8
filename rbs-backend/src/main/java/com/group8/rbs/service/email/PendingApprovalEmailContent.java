package com.group8.rbs.service.email;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class PendingApprovalEmailContent implements EmailContentStrategy {
    @Override
    public String buildSubject(Map<String, Object> params) {
        return "RBS Booking Confirmation";
    }

    @Override
    public String buildBody(Map<String, Object> params) {
        LocalDateTime bookedDateTime = (LocalDateTime) params.get("bookedDatetime");
        String formattedDate = bookedDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return "<html><body>" +
                "<p>Dear Student,</p>" +
                "<p>Your booking is currently pending approval.</p>" +
                "<p><strong>Booking ID:</strong> " + params.get("bookingId") + "</p>" +
                "<p><strong>Date:</strong> " + formattedDate + "</p>" +
                "<p><strong>Facility:</strong> " + params.get("facilityName") + "</p>" +
                "<p><strong>Timeslot:</strong> " + params.get("timeslot") + "</p>" +
                "<p><strong>Status:</strong> " + params.get("status") + "</p>" +
                "<p>Best regards,<br>Resource Booking System</p>" +
                "</body></html>";
    }
}