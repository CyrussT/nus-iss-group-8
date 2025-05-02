package com.group8.rbs.validation;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.entities.Account;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Validates that the booking is not in the past.
 */
@Component
public class PastDateTimeValidator extends BaseBookingValidator {
    
    @Override
    public ValidationResult validate(BookingDTO booking, Account account) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime bookingTime = booking.getBookedDateTime();
        
        if (bookingTime.isBefore(now)) {
            return ValidationResult.failure("Cannot create bookings in the past");
        }
        
        return checkNext(booking, account);
    }
}