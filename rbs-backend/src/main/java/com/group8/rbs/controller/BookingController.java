package com.group8.rbs.controller;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.dto.booking.BookingRequestDTO;
import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.service.booking.BookingService;
import com.group8.rbs.service.email.CustomEmailService;

import jakarta.mail.MessagingException;

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
    private final CustomEmailService emailService;

    public BookingController(BookingService bookingService, CustomEmailService emailService) {
        this.bookingService = bookingService;
        this.emailService = emailService;
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

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody BookingDTO request) {
        System.out.println("Request: " + request);
        BookingResponseDTO response = bookingService.createBooking(request);
        return ResponseEntity.ok(response);
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
        List<BookingResponseDTO> history = bookingService.getBookingHistory(request.getStudentId(),
                request.getStatus());
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/cancel-booking/{bookingId}")
    public ResponseEntity<String> deleteBooking(@PathVariable Long bookingId, @RequestParam String toEmail)
            throws MessagingException {
        boolean isDeleted = bookingService.deleteBooking(bookingId);

        if (isDeleted) {
            String subject = "RBS Booking Cancellation Confirmation";
            String body = "<html>" +
                    "<body>" +
                    "<p>Dear Student,</p>" +
                    "<p>Your booking has been successfully canceled.</p>" +
                    "<p><strong>Booking ID:</strong> " + bookingId + "</p>" +
                    "<p>Best regards,</p>" +
                    "<p>Resource Booking System</p>" +
                    "</body>" +
                    "</html>";

            boolean emailSent = emailService.sendEmail(toEmail, subject, body);

            if (emailSent) {
                System.out.println("Email sent successfully after cancellation.");
                return ResponseEntity.ok("Booking deleted successfully and confirmation email sent.");
            } else {
                System.out.println("Failed to send email after cancellation.");
                return ResponseEntity.ok("Booking deleted successfully, but failed to send confirmation email.");
            }
        } else {
            return ResponseEntity.status(404).body("Booking not found.");
        }
    }

    @GetMapping("/send-test-email")
    public String sendTestEmail(@RequestParam String toEmail) throws MessagingException {
        String subject = "Test Email Subject";
        String body = "This is a test email sent from the Spring Boot application.";

        boolean emailSent = emailService.sendEmail(toEmail, subject, body);

        if (emailSent) {
            return "Test email sent successfully to " + toEmail;
        } else {
            return "Failed to send test email to " + toEmail;
        }
    }
}
