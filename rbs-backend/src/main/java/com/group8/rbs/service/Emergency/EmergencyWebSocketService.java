package com.group8.rbs.service.Emergency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmergencyWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public EmergencyWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendEmergencyAlert(String message) {
        messagingTemplate.convertAndSend("/topic/emergency", message);
    }
}
