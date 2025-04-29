package com.group8.rbs.validation;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.repository.FacilityRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Validates that the requested facility exists.
 */
@Component
public class FacilityExistsValidator extends BaseBookingValidator {
    private final FacilityRepository facilityRepository;
    
    public FacilityExistsValidator(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }
    
    @Override
    public ValidationResult validate(BookingDTO booking, Account account) {
        Optional<Facility> facility = facilityRepository.findById(booking.getFacilityId());
        
        if (facility.isEmpty()) {
            return ValidationResult.failure("Facility not found");
        }
        
        // Store facility in context for later validators to use
        BookingValidationContext.setFacility(facility.get());
        return checkNext(booking, account);
    }
}