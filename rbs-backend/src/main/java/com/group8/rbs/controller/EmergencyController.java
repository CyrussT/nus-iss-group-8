package com.group8.rbs.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.group8.rbs.service.Emergency.EmergencyWebSocketService;

@RestController
@RequestMapping("/api/emergency")
public class EmergencyController {

    private final EmergencyWebSocketService emergencyWebSocketService;

    @Autowired
    public EmergencyController(EmergencyWebSocketService emergencyWebSocketService) {
        this.emergencyWebSocketService = emergencyWebSocketService;
    }

    @PostMapping("/push")
    public ResponseEntity<String> pushEmergencyNotice(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message cannot be empty");
        }

        emergencyWebSocketService.sendEmergencyAlert(message);
        return ResponseEntity.ok("Emergency notice pushed successfully");
    }
}
