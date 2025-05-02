package com.group8.rbs.validation;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.entities.Account;
import org.springframework.stereotype.Component;

/**
 * Validates that the booking has a title.
 */
@Component
public class TitleValidator extends BaseBookingValidator {
    
    @Override
    public ValidationResult validate(BookingDTO booking, Account account) {
        if (booking.getTitle() == null || booking.getTitle().trim().isEmpty()) {
            return ValidationResult.failure("Booking title is required");
        }
        
        return checkNext(booking, account);
    }
}