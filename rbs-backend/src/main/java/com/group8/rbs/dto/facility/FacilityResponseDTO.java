package com.group8.rbs.dto.facility;

import java.util.List;

import com.group8.rbs.dto.booking.BookingResponseDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacilityResponseDTO {
    private Long facilityId;
    private Long resourceTypeId;
    private String resourceName;
    private String location;
    private Integer capacity;
    private String message;
    private List<BookingResponseDTO> bookings;
}
