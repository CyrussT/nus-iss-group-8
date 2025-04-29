package com.group8.rbs.service.maintenance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MaintenanceWebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendMaintenanceUpdate(Long facilityId) {
        messagingTemplate.convertAndSend("/topic/maintenance", facilityId); 
    }
}
