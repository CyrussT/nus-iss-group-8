package com.group8.rbs.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CancellationEmailContentTest {

    private CancellationEmailContent cancellationEmailContent;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        cancellationEmailContent = new CancellationEmailContent();
        params = new HashMap<>();

        params.put("bookingId", "B123");
    }

    @Test
    void testBuildSubject() {
        String subject = cancellationEmailContent.buildSubject(params);

        assertEquals("RBS Booking Cancellation", subject);
    }

    @Test
    void testBuildBody_containsBookingId() {
        String body = cancellationEmailContent.buildBody(params);

        assertTrue(body.contains("Booking ID:</strong> B123"));
    }

    @Test
    void testBuildBody_missingBookingId() {
        params.remove("bookingId");
        String body = cancellationEmailContent.buildBody(params);

        assertTrue(body.contains("Booking ID:"));
    }

    @Test
    void testBuildBody_handlesMissingOptionalFieldsGracefully() {
        params.clear();

        String body = cancellationEmailContent.buildBody(params);

        assertTrue(body.contains("Booking ID:"));
    }
}
