package com.group8.rbs.service.booking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookingWebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendBookingUpdate(Long bookingId) {
        messagingTemplate.convertAndSend("/topic/bookings", bookingId); // notify all clients
    }
}
