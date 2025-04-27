package com.group8.rbs.validation;

import org.springframework.stereotype.Component;

/**
 * Builder for constructing the validator chain with appropriate order.
 */
@Component
public class BookingValidationChainBuilder {
    private final FacilityExistsValidator facilityValidator;
    private final MaintenanceValidator maintenanceValidator;
    private final TimeSlotValidator timeSlotValidator;
    private final CreditValidator creditValidator;
    private final EndTimeValidator endTimeValidator;
    private final TitleValidator titleValidator;
    private final PastDateTimeValidator pastDateTimeValidator;
    
    public BookingValidationChainBuilder(
            PastDateTimeValidator pastDateTimeValidator,
            TitleValidator titleValidator,
            EndTimeValidator endTimeValidator,
            FacilityExistsValidator facilityValidator,
            MaintenanceValidator maintenanceValidator,
            CreditValidator creditValidator,
            TimeSlotValidator timeSlotValidator) {
        this.pastDateTimeValidator = pastDateTimeValidator;
        this.titleValidator = titleValidator;
        this.endTimeValidator = endTimeValidator;
        this.facilityValidator = facilityValidator;
        this.maintenanceValidator = maintenanceValidator;
        this.creditValidator = creditValidator;
        this.timeSlotValidator = timeSlotValidator;
    }
    
    /**
     * Builds the validation chain in the optimal order.
     */
    public BookingValidator buildValidationChain() {
        // Start with trivial validations that are fast and might fail
        pastDateTimeValidator.setNext(titleValidator);
        titleValidator.setNext(endTimeValidator);
        endTimeValidator.setNext(facilityValidator);
        facilityValidator.setNext(maintenanceValidator);
        maintenanceValidator.setNext(creditValidator);
        creditValidator.setNext(timeSlotValidator);
        
        return pastDateTimeValidator;
    }
}