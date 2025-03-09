package com.group8.rbs.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private Long facilityId;
    private String accountEmail; // User creating the booking
    private LocalDateTime bookedDateTime; // Date of the booking
    private String timeSlot; // Format: "09:00 - 10:00"
    private String title; // not sure if storing this
    private String description; // not sure if storing this
    private String attendees; // not sure if storing this
}