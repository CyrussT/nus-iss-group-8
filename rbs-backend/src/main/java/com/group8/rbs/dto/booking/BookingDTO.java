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
    
    /**
     * Date and time of the booking in Singapore timezone.
     * This datetime is expected to be provided by the frontend in Singapore timezone.
     */
    private LocalDateTime bookedDateTime;
    
    private String timeSlot; // Format: "09:00 - 10:00"
    private String title;
    private String description;
    private String creditsUsed; // Credits used for the booking
}