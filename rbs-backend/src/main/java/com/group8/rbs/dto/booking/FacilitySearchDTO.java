package com.group8.rbs.dto.booking;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitySearchDTO {
    private Long facilityId;
    private Long resourceTypeId;
    private String resourceName;
    private String location;
    private Integer capacity;
    private LocalDate date;
    private List<BookingResponseDTO> bookings;
}