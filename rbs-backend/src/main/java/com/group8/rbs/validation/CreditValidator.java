package com.group8.rbs.validation;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.entities.Account;
import com.group8.rbs.repository.CreditRepository;
import org.springframework.stereotype.Component;

/**
 * Validates that the user has sufficient credits for the booking.
 */
@Component
public class CreditValidator extends BaseBookingValidator {
    private final CreditRepository creditRepository;
    
    public CreditValidator(CreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }
    
    @Override
    public ValidationResult validate(BookingDTO booking, Account account) {
        // Parse the credits needed from the request
        Double creditsNeeded;
        try {
            creditsNeeded = Double.parseDouble(booking.getCreditsUsed());
        } catch (NumberFormatException e) {
            return ValidationResult.failure("Invalid credits used value: " + booking.getCreditsUsed());
        }
        
        // Get current balance for comparison
        Double currentBalance = creditRepository.findCreditBalanceByAccountId(account.getAccountId());
        
        if (currentBalance < creditsNeeded) {
            return ValidationResult.failure("Insufficient credits. Required: " + creditsNeeded + 
                                           ", Available: " + currentBalance);
        }
        
        return checkNext(booking, account);
    }
}