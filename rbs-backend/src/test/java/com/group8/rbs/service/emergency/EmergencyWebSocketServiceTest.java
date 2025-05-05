package com.group8.rbs.service.emergency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.group8.rbs.service.Emergency.EmergencyWebSocketService;

import static org.mockito.Mockito.*;

class EmergencyWebSocketServiceTest {

    private SimpMessagingTemplate messagingTemplate;
    private EmergencyWebSocketService emergencyWebSocketService;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        emergencyWebSocketService = new EmergencyWebSocketService(messagingTemplate);
    }

    @Test
    void testSendEmergencyAlert() {
        String message = "Test emergency alert";

        emergencyWebSocketService.sendEmergencyAlert(message);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/emergency", message);
    }
}
