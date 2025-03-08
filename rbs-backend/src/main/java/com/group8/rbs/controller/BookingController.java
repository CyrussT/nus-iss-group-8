package com.group8.rbs.controller;

import com.group8.rbs.dto.booking.BookingRequestDTO;
import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.service.booking.BookingService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

     @GetMapping("/facilities/search")
    public ResponseEntity<List<FacilitySearchDTO>> searchFacilities(
            @RequestParam(required = false) Long facilityId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceName,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        FacilitySearchDTO searchCriteria = FacilitySearchDTO.builder()
                .facilityId(facilityId)
                .resourceType(resourceType)
                .resourceName(resourceName)
                .location(location)
                .capacity(capacity)
                .date(date)
                .build();
        
        // Always use the date-filtered search
        return ResponseEntity.ok(bookingService.searchFacilities(searchCriteria));
    }

    @GetMapping("/dropdown-options")
    public ResponseEntity<Map<String, List<String>>> getDropdownOptions() {
        Map<String, List<String>> options = bookingService.getDropdownOptions();
        return ResponseEntity.ok(options);
    }

    @GetMapping("/upcoming-approved")
    public ResponseEntity<List<BookingResponseDTO>> getUpcomingApprovedBookings(@RequestParam Long accountId) {
        List<BookingResponseDTO> upcomingBookings = bookingService.getUpcomingApprovedBookings(accountId);
        return ResponseEntity.ok(upcomingBookings);
    }

    // Fetch pending bookings where the date is in the future
    @GetMapping("/pending-future")
    public ResponseEntity<List<BookingResponseDTO>> getPendingFutureBookings(@RequestParam Long accountId) {
        List<BookingResponseDTO> bookings = bookingService.getPendingFutureBookings(accountId);
        return ResponseEntity.ok(bookings);
    }


    // Fetch booking history for a student (can only see its own booking history)
    @PostMapping("/history")
    public ResponseEntity<List<BookingResponseDTO>> getBookingHistory(@RequestBody BookingRequestDTO request) {
        List<BookingResponseDTO> history = bookingService.getBookingHistory(request.getStudentId(), request.getStatus());
        return ResponseEntity.ok(history);
    }

    

}
