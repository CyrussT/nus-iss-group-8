package com.group8.rbs.validation;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.repository.BookingRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Validates that the requested time slot is available.
 */
@Component
public class TimeSlotValidator extends BaseBookingValidator {
    private final BookingRepository bookingRepository;
    
    public TimeSlotValidator(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }
    
    @Override
    public ValidationResult validate(BookingDTO booking, Account account) {
        // Extract date part from bookedDateTime
        LocalDate bookingDate = booking.getBookedDateTime().toLocalDate();
        
        // Create start and end of day for the date range query
        LocalDateTime startOfDay = bookingDate.atStartOfDay();
        LocalDateTime endOfDay = bookingDate.plusDays(1).atStartOfDay().minusNanos(1);
        
        // Query existing bookings for this facility on this date
        List<Booking> existingBookings = bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                booking.getFacilityId(),
                startOfDay,
                endOfDay,
                Arrays.asList(BookingStatus.APPROVED, BookingStatus.CONFIRMED, BookingStatus.PENDING));
        
        // Check for overlap with the requested time slot
        for (Booking existingBooking : existingBookings) {
            if (existingBooking.getTimeSlot().equals(booking.getTimeSlot())) {
                return ValidationResult.failure("This time slot is already booked");
            }
        }
        
        return checkNext(booking, account);
    }
}