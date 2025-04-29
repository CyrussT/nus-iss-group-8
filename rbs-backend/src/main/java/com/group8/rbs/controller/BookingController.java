package com.group8.rbs.controller;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.dto.booking.BookingRequestDTO;
import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.service.booking.BookingService;
import com.group8.rbs.service.email.EmailContentStrategy;
import com.group8.rbs.service.email.EmailContentStrategyFactory;
import com.group8.rbs.service.email.EmailService;
import com.group8.rbs.service.email.EmailServiceFactory;
import com.group8.rbs.service.maintenance.MaintenanceService;

import jakarta.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
    private final BookingService bookingService;
    private static final ZoneId SG_ZONE = ZoneId.of("Asia/Singapore");
    private final EmailServiceFactory emailServiceFactory;
    private final EmailContentStrategyFactory emailContentStrategyFactory;
    private final MaintenanceService maintenanceService;


    public BookingController(BookingService bookingService,
            EmailServiceFactory emailServiceFactory,
            EmailContentStrategyFactory emailContentStrategyFactory,
            MaintenanceService maintenanceService) {
        this.bookingService = bookingService;
        this.emailServiceFactory = emailServiceFactory;
        this.emailContentStrategyFactory = emailContentStrategyFactory;
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/admin/dashboard-stats")
public ResponseEntity<Map<String, Object>> getDashboardStats() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("todayBookings", bookingService.countTodayBookings());
    stats.put("pendingApprovals", bookingService.countPendingBookings());
    stats.put("facilitiesUnderMaintenance", maintenanceService.countFacilitiesUnderMaintenanceToday());
    // If you implement Emergency Announcements: add "latestAnnouncement" here too
    
    return ResponseEntity.ok(stats);
}

    @GetMapping("/facilities/search")
    public ResponseEntity<List<FacilitySearchDTO>> searchFacilities(
            @RequestParam(required = false) Long facilityId,
            @RequestParam(required = false) Long resourceTypeId,
            @RequestParam(required = false) String resourceName,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        FacilitySearchDTO searchCriteria = FacilitySearchDTO.builder()
                .facilityId(facilityId)
                .resourceTypeId(resourceTypeId)
                .resourceName(resourceName)
                .location(location)
                .capacity(capacity)
                .date(date)
                .build();

        // Always use the date-filtered search
        return ResponseEntity.ok(bookingService.searchFacilities(searchCriteria));
    }

    @GetMapping("/dropdown-options")
    public ResponseEntity<Map<String, Object>> getDropdownOptions() {
        Map<String, Object> options = bookingService.getDropdownOptions();
        return ResponseEntity.ok(options);
    }

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody BookingDTO request) throws MessagingException {
        logger.info("Received booking request: " + request);

        // If the bookedDateTime has a timezone offset in the string (like +08:00),
        // it will be parsed correctly by default. If not, we assume it's in Singapore
        // time
        LocalDateTime bookingDateTime = request.getBookedDateTime();

        // Log the parsed datetime for debugging
        logger.info("Parsed booking datetime: " + bookingDateTime);

        BookingResponseDTO response = bookingService.createBooking(request);

        if (response != null && response.getBookingId() != null) {
            // Construct email details
            String toEmail = request.getAccountEmail();
            String status = response.getStatus();
            String strategyKey = status.equalsIgnoreCase("PENDING") ? "PENDING" : "SYSTEMAPPROVED";

            // Fetch the email content strategy using the factory
            EmailContentStrategy strategy = emailContentStrategyFactory.getStrategy(strategyKey);

            // Prepare the parameters for email content (e.g., bookingId, reason if needed)
            Map<String, Object> emailParams = new HashMap<>();
            emailParams.put("bookingId", response.getBookingId());
            emailParams.put("bookedDatetime", response.getBookedDatetime()); // Add the bookedDatetime
            emailParams.put("facilityName", response.getFacilityName());
            emailParams.put("timeslot", response.getTimeslot());
            emailParams.put("status", response.getStatus());

            String subject = strategy.buildSubject(emailParams);
            String body = strategy.buildBody(emailParams);

            // Send email
            EmailService emailService = emailServiceFactory.getEmailService("customEmailService");
            boolean emailSent = emailService.sendEmail(toEmail, subject, body);

            if (emailSent) {
                logger.info("Booking confirmation email sent successfully.");
            } else {
                logger.info("Failed to send booking confirmation email.");
            }

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(500).body(null);
        }
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
            EmailContentStrategy strategy = emailContentStrategyFactory.getStrategy("CANCELLED");

            Map<String, Object> emailParams = new HashMap<>();
            emailParams.put("bookingId", bookingId);

            String subject = strategy.buildSubject(emailParams);
            String body = strategy.buildBody(emailParams);

            EmailService emailService = emailServiceFactory.getEmailService("customEmailService");
            boolean emailSent = emailService.sendEmail(toEmail, subject, body);
            // up user credit

            if (emailSent) {
                logger.info("Email sent successfully after cancellation.");
                return ResponseEntity.ok("Booking deleted successfully and confirmation email sent.");
            } else {
                logger.info("Failed to send email after cancellation.");
                return ResponseEntity.ok("Booking deleted successfully, but failed to send confirmation email.");
            }
        } else {
            return ResponseEntity.status(404).body("Booking not found.");
        }
    }

    @GetMapping("/pending-bookings")
    public ResponseEntity<List<BookingResponseDTO>> getPendingBookings() {
        List<BookingResponseDTO> pendingBookings = bookingService.getBookingsByStatus("PENDING");

        if (pendingBookings.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(pendingBookings);
    }

    @PutMapping("/update/{bookingId}")
    public ResponseEntity<String> updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestParam String toEmail,
            @RequestParam BookingStatus status,
            @RequestParam(required = false) String rejectReason) throws MessagingException {

        boolean updated = bookingService.updateBookingStatus(bookingId, status);

        if (updated && status.equals(BookingStatus.REJECTED)) {
            EmailContentStrategy strategy = emailContentStrategyFactory.getStrategy("REJECTED");
            Map<String, Object> emailParams = new HashMap<>();
            emailParams.put("bookingId", bookingId);
            emailParams.put("reason", rejectReason);

            String subject = strategy.buildSubject(emailParams);
            String body = strategy.buildBody(emailParams);

            EmailService emailService = emailServiceFactory.getEmailService("customEmailService");
            boolean emailSent = emailService.sendEmail(toEmail, subject, body);

            if (emailSent) {
                logger.info("Email sent successfully after rejection.");
                return ResponseEntity.ok("Booking rejected successfully and rejection email sent.");
            } else {
                logger.info("Failed to send email after rejection.");
                return ResponseEntity.ok("Booking rejected successfully, but failed to send rejection email.");
            }
        }

        else if (updated && status.equals(BookingStatus.APPROVED)) {
            EmailContentStrategy strategy = emailContentStrategyFactory.getStrategy("ADMINPPROVED");

            Map<String, Object> emailParams = new HashMap<>();
            emailParams.put("bookingId", bookingId);

            String subject = strategy.buildSubject(emailParams);
            String body = strategy.buildBody(emailParams);

            EmailService emailService = emailServiceFactory.getEmailService("customEmailService");
            boolean emailSent = emailService.sendEmail(toEmail, subject, body);

            if (emailSent) {
                return ResponseEntity.ok("Booking approved successfully and approval email sent.");
            } else {
                return ResponseEntity.ok("Booking approved successfully, but failed to send approval email.");
            }
        }

        else {
            return ResponseEntity.status(404).body("Booking not found.");
        }
    }
}