package com.group8.rbs.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PendingApprovalEmailContentTest {

    private PendingApprovalEmailContent pendingApprovalEmailContent;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        pendingApprovalEmailContent = new PendingApprovalEmailContent();
        params = new HashMap<>();

        params.put("bookingId", "B123");
        params.put("facilityName", "Study Room A");
        params.put("timeslot", "2PM - 4PM");
        params.put("status", "Pending");

        params.put("bookedDatetime", LocalDateTime.of(2025, 4, 29, 14, 0));
    }

    @Test
    void testBuildSubject() {
        String subject = pendingApprovalEmailContent.buildSubject(params);

        assertEquals("RBS Booking Confirmation", subject);
    }

    @Test
    void testBuildBody_containsAllFields() {
        String body = pendingApprovalEmailContent.buildBody(params);

        assertTrue(body.contains("Booking ID:</strong> B123"));
        assertTrue(body.contains("Date:</strong> 2025-04-29"));
        assertTrue(body.contains("Facility:</strong> Study Room A"));
        assertTrue(body.contains("Timeslot:</strong> 2PM - 4PM"));
        assertTrue(body.contains("Status:</strong> Pending"));
    }

    @Test
    void testBuildBody_missingBookingId() {
        params.remove("bookingId");
        String body = pendingApprovalEmailContent.buildBody(params);

        assertTrue(body.contains("Booking ID:"));
    }

    @Test
    void testBuildBody_missingFacilityName() {
        params.remove("facilityName");
        String body = pendingApprovalEmailContent.buildBody(params);

        assertTrue(body.contains("Facility:"));
    }

    @Test
    void testBuildBody_missingTimeslot() {
        params.remove("timeslot");
        String body = pendingApprovalEmailContent.buildBody(params);

        assertTrue(body.contains("Timeslot:"));
    }

    @Test
    void testBuildBody_missingStatus() {
        params.remove("status");
        String body = pendingApprovalEmailContent.buildBody(params);

        assertTrue(body.contains("Status:"));
    }

    @Test
    void testBuildBody_handlesMissingOptionalFieldsGracefully() {
        params.remove("bookingId");
        params.remove("facilityName");
        params.remove("timeslot");
        params.remove("status");

        String body = pendingApprovalEmailContent.buildBody(params);

        assertTrue(body.contains("Booking ID:"));
        assertTrue(body.contains("Facility:"));
        assertTrue(body.contains("Timeslot:"));
        assertTrue(body.contains("Status:"));
    }
}
