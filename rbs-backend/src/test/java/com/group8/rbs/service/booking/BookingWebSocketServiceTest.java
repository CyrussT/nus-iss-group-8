package com.group8.rbs.service.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

class BookingWebSocketServiceTest {

    private SimpMessagingTemplate messagingTemplate;
    private BookingWebSocketService bookingWebSocketService;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        bookingWebSocketService = new BookingWebSocketService();
        // Since the field is autowired, manually inject the mock
        bookingWebSocketService.messagingTemplate = messagingTemplate;
    }

    @Test
    void testSendBookingUpdate() {
        Long bookingId = 456L;

        bookingWebSocketService.sendBookingUpdate(bookingId);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/bookings", bookingId);
    }
}
