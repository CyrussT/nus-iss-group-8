package com.group8.rbs.mapper;

import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.entities.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {
    public BookingResponseDTO toResponseDTO(Booking booking) {
        return BookingResponseDTO.builder()
                .bookingId(booking.getBookingId())
                .facilityName(booking.getFacility().getResourceName())
                .location(booking.getFacility().getLocation())
                .bookedDatetime(booking.getBookedDateTime())
                .timeslot(booking.getTimeSlot())
                .status(booking.getStatus().toString())
                .title(booking.getTitle())
                .description(booking.getDescription())
                .studentId(booking.getAccount().getStudentId())
                .studentName(booking.getAccount().getName())
                .email(booking.getAccount().getEmail())
                .build();
    }
}
