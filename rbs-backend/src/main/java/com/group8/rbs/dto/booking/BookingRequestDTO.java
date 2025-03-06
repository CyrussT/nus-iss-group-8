package com.group8.rbs.dto.booking;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingRequestDTO {
    private Long studentId;
    private String status; // Optional filter (e.g., "Completed", "Cancelled")
}