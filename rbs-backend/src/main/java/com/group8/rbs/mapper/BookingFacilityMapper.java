package com.group8.rbs.mapper;

import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.enums.BookingStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookingFacilityMapper {
    
    private final BookingMapper bookingMapper;
    
    public BookingFacilityMapper(BookingMapper bookingMapper) {
        this.bookingMapper = bookingMapper;
    }
    
    public FacilitySearchDTO toResponseDTO(Facility facility) {
        return toResponseDTOWithDateFilter(facility, null);
    }
    
    // Overloaded method with date filtering capability
    public FacilitySearchDTO toResponseDTOWithDateFilter(Facility facility, LocalDate filterDate) {
        List<BookingResponseDTO> bookingResponseDTOs = Collections.emptyList();
        
        if (facility.getBookings() != null && !facility.getBookings().isEmpty()) {
            // Get a stream of bookings
            var bookingStream = facility.getBookings().stream()
                .filter(booking -> 
                    booking.getStatus() == BookingStatus.APPROVED || 
                    booking.getStatus() == BookingStatus.CONFIRMED ||
                    booking.getStatus() == BookingStatus.PENDING);
            
            // Apply date filter if specified
            if (filterDate != null) {
                bookingStream = bookingStream.filter(booking -> 
                    booking.getBookedDateTime().toLocalDate().equals(filterDate));
            }
            
            // Convert to DTOs
            bookingResponseDTOs = bookingStream
                .map(bookingMapper::toResponseDTO)
                .collect(Collectors.toList());
        }
        
        return FacilitySearchDTO.builder()
                .facilityId(facility.getFacilityId())
                .resourceType(facility.getResourceType())
                .resourceName(facility.getResourceName())
                .location(facility.getLocation())
                .capacity(facility.getCapacity())
                .bookings(bookingResponseDTOs)
                .build();
    }
}