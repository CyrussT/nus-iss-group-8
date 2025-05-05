package com.group8.rbs.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SystemApprovedEmailContentTest {

    private SystemApprovedEmailContent emailContent;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        emailContent = new SystemApprovedEmailContent();

        params = new HashMap<>();
        params.put("bookingId", "B123");
        params.put("bookedDatetime", LocalDateTime.of(2025, 4, 29, 14, 0));
        params.put("facilityName", "Study Room A");
        params.put("timeslot", "2PM - 4PM");
        params.put("status", "Approved");
    }

    @Test
    void testBuildSubject() {
        String subject = emailContent.buildSubject(params);
        assertEquals("RBS Booking Confirmation", subject);
    }

    @Test
    void testBuildBody_containsAllFields() {
        String body = emailContent.buildBody(params);

        assertTrue(body.contains("Dear Student"));
        assertTrue(body.contains("Booking ID:</strong> B123"));
        assertTrue(body.contains("Date:</strong> 2025-04-29"));
        assertTrue(body.contains("Facility:</strong> Study Room A"));
        assertTrue(body.contains("Timeslot:</strong> 2PM - 4PM"));
        assertTrue(body.contains("Status:</strong> Approved"));
        assertTrue(body.contains("Resource Booking System"));
    }

}
