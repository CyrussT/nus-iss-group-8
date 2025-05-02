package com.group8.rbs.validation;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.entities.Account;

/**
 * Base interface for all booking validators in the chain.
 */
public interface BookingValidator {
    ValidationResult validate(BookingDTO booking, Account account);
    void setNext(BookingValidator nextValidator);
}