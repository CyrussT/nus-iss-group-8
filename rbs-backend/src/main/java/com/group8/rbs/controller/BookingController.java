package com.group8.rbs.controller;

import com.group8.rbs.dto.booking.BookingRequestDTO;
import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.service.booking.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/upcoming-approved")
    public ResponseEntity<List<BookingResponseDTO>> getUpcomingApprovedBookings(@RequestParam Long accountId) {
        List<BookingResponseDTO> upcomingBookings = bookingService.getUpcomingApprovedOrConfirmedBookings(accountId);
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
