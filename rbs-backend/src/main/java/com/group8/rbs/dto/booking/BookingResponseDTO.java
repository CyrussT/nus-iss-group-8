package com.group8.rbs.dto.booking;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingResponseDTO {
    private Long bookingId;
    private String studentId;
    private String studentName;
    private String facilityName;
    private String location;
    private LocalDateTime bookedDatetime;
    private String timeslot;
    private String status;
    private String email;
}
