package com.group8.rbs.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class EmailServiceFactoryTest {

    private EmailServiceFactory emailServiceFactory;

    @Mock
    private Map<String, EmailService> mockEmailServices;

    @Mock
    private EmailService customEmailService;

    @Mock
    private EmailService anotherEmailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        emailServiceFactory = new EmailServiceFactory(mockEmailServices);
    }

    @Test
    void testGetEmailService_returnsCorrectService() {
        when(mockEmailServices.get("another")).thenReturn(anotherEmailService);
        when(mockEmailServices.get("customEmailService")).thenReturn(customEmailService);

        EmailService result = emailServiceFactory.getEmailService("another");

        System.out.println(result);
        assertNotNull(result, "Expected 'result' to be non-null.");
        assertEquals(anotherEmailService, result, "Expected 'anotherEmailService' to be returned.");
    }

    @Test
    void testGetEmailService_returnsCustomService_whenTypeNotFound() {
        when(mockEmailServices.get("another")).thenReturn(null);
        when(mockEmailServices.get("customEmailService")).thenReturn(customEmailService);

        EmailService result = emailServiceFactory.getEmailService("another");

        assertNotNull(result, "Expected 'result' to be non-null.");
        assertEquals(customEmailService, result, "Expected 'customEmailService' to be returned.");
    }

    @Test
    void testGetEmailService_returnsDefaultService_whenNullMap() {
        // Mock the behavior of the map for both keys as null (i.e., both keys don't
        // exist)
        when(mockEmailServices.get("another")).thenReturn(null);
        when(mockEmailServices.get("customEmailService")).thenReturn(null);

        // Act: Call getEmailService for the "another" type
        EmailService result = emailServiceFactory.getEmailService("another");

        // Assert: Verify that the result is null when no service is found
        assertNull(result, "Expected 'result' to be null.");
    }
}
