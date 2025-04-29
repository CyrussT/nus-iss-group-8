package com.group8.rbs.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RejectionEmailContentTest {

    private RejectionEmailContent rejectionEmailContent;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        rejectionEmailContent = new RejectionEmailContent();
        params = new HashMap<>();

        params.put("bookingId", "B123");
        params.put("reason", "Overbooking");
    }

    @Test
    void testBuildSubject() {
        String subject = rejectionEmailContent.buildSubject(params);

        assertEquals("RBS Booking Rejected", subject);
    }

    @Test
    void testBuildBody_containsBookingIdAndReason() {
        String body = rejectionEmailContent.buildBody(params);
        System.out.println(body);
        assertTrue(body.contains("Booking ID:</strong> B123"));
        assertTrue(body.contains("Reason:</strong> Overbooking"));
    }

    @Test
    void testBuildBody_missingReason() {
        params.remove("reason");
        String body = rejectionEmailContent.buildBody(params);
        
        assertTrue(body.contains("Reason:"));
    }

    @Test
    void testBuildBody_missingBookingId() {
        params.remove("bookingId");
        String body = rejectionEmailContent.buildBody(params);
        System.out.println(body);

        assertTrue(body.contains("Booking ID:")); 
    }

    @Test
    void testBuildBody_handlesMissingOptionalFieldsGracefully() {
        params.remove("reason");
        params.remove("bookingId");

        String body = rejectionEmailContent.buildBody(params);

        assertTrue(body.contains("Booking ID:"));
        assertTrue(body.contains("Reason:"));
    }
}
