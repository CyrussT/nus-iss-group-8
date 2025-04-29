package com.group8.rbs.validation;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.entities.Account;

/**
 * Abstract base class implementing common validator functionality.
 */
public abstract class BaseBookingValidator implements BookingValidator {
    protected BookingValidator next;
    
    @Override
    public void setNext(BookingValidator nextValidator) {
        this.next = nextValidator;
    }
    
    /**
     * Helper method to continue the chain if this validation passes.
     */
    protected ValidationResult checkNext(BookingDTO booking, Account account) {
        if (next != null) {
            return next.validate(booking, account);
        }
        return ValidationResult.success();
    }
}