package com.group8.rbs.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AdminApprovedEmailContentTest {

    private AdminApprovedEmailContent adminApprovedEmailContent;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        adminApprovedEmailContent = new AdminApprovedEmailContent();
        params = new HashMap<>();

        params.put("bookingId", "B123");
    }

    @Test
    void testBuildSubject() {
        String subject = adminApprovedEmailContent.buildSubject(params);

        assertEquals("RBS Booking Approved", subject);
    }

    @Test
    void testBuildBody_containsBookingId() {
        String body = adminApprovedEmailContent.buildBody(params);

        assertTrue(body.contains("Booking ID:</strong> B123"));
    }

    @Test
    void testBuildBody_missingBookingId() {
        params.remove("bookingId");
        String body = adminApprovedEmailContent.buildBody(params);

        assertTrue(body.contains("Booking ID:"));
    }

    @Test
    void testBuildBody_handlesMissingOptionalFieldsGracefully() {
        params.clear();

        String body = adminApprovedEmailContent.buildBody(params);

        assertTrue(body.contains("Booking ID:"));
    }
}
