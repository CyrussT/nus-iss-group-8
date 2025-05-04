package com.group8.rbs.controller;

import com.group8.rbs.service.Emergency.EmergencyWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmergencyControllerTest {

    private EmergencyWebSocketService emergencyWebSocketService;
    private EmergencyController emergencyController;

    @BeforeEach
    void setUp() {
        emergencyWebSocketService = mock(EmergencyWebSocketService.class);
        emergencyController = new EmergencyController(emergencyWebSocketService);
    }

    @Test
    void testPushEmergencyNotice_success() {
        Map<String, String> request = new HashMap<>();
        request.put("message", "This is a test emergency");

        ResponseEntity<String> response = emergencyController.pushEmergencyNotice(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Emergency notice pushed successfully", response.getBody());
        verify(emergencyWebSocketService, times(1)).sendEmergencyAlert("This is a test emergency");
    }

    @Test
    void testPushEmergencyNotice_emptyMessage() {
        Map<String, String> request = new HashMap<>();
        request.put("message", "  "); // empty after trim

        ResponseEntity<String> response = emergencyController.pushEmergencyNotice(request);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Message cannot be empty", response.getBody());
        verify(emergencyWebSocketService, never()).sendEmergencyAlert(any());
    }

    @Test
    void testPushEmergencyNotice_missingMessageKey() {
        Map<String, String> request = new HashMap<>(); // no "message" key

        ResponseEntity<String> response = emergencyController.pushEmergencyNotice(request);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Message cannot be empty", response.getBody());
        verify(emergencyWebSocketService, never()).sendEmergencyAlert(any());
    }
}
