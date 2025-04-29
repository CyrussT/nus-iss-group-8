package com.group8.rbs.validation;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.entities.Account;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Validates that the booking does not end after 7 PM (closing time).
 */
@Component
public class EndTimeValidator extends BaseBookingValidator {
    
    @Override
    public ValidationResult validate(BookingDTO booking, Account account) {
        // Get the booking start time
        LocalDateTime startTime = booking.getBookedDateTime();
        
        // Parse the duration from creditsUsed (minutes)
        int durationMinutes;
        try {
            // In your system, credits seem to be measured in minutes
            durationMinutes = (int)Double.parseDouble(booking.getCreditsUsed());
        } catch (NumberFormatException e) {
            return ValidationResult.failure("Invalid duration value");
        }
        
        // Calculate end time
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
        
        // Create a reference to 7 PM on the same day
        LocalDateTime sevenPM = startTime.toLocalDate().atTime(LocalTime.of(19, 0));
        
        // Check if end time is after 7 PM
        if (endTime.isAfter(sevenPM)) {
            return ValidationResult.failure("Bookings cannot extend beyond 7:00 PM closing time");
        }
        
        return checkNext(booking, account);
    }
}