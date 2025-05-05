package com.group8.rbs.service.maintenance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

class MaintenanceWebSocketServiceTest {

    private SimpMessagingTemplate messagingTemplate;
    private MaintenanceWebSocketService maintenanceWebSocketService;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        maintenanceWebSocketService = new MaintenanceWebSocketService();
        // Use reflection or setter injection since field is @Autowired
        maintenanceWebSocketService.messagingTemplate = messagingTemplate;
    }

    @Test
    void testSendMaintenanceUpdate() {
        Long facilityId = 123L;

        maintenanceWebSocketService.sendMaintenanceUpdate(facilityId);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/maintenance", facilityId);
    }
}
