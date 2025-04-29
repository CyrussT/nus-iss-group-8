package com.group8.rbs.validation;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.entities.Account;
import com.group8.rbs.service.maintenance.MaintenanceService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Validates that the facility is not under maintenance.
 */
@Component
public class MaintenanceValidator extends BaseBookingValidator {
    private final MaintenanceService maintenanceService;
    
    public MaintenanceValidator(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }
    
    @Override
    public ValidationResult validate(BookingDTO booking, Account account) {
        LocalDate bookingDate = booking.getBookedDateTime().toLocalDate();
        
        boolean underMaintenance = maintenanceService.isFacilityUnderMaintenanceOnDate(
            booking.getFacilityId(), 
            bookingDate
        );
        
        if (underMaintenance) {
            return ValidationResult.failure("This facility is under maintenance and cannot be booked on the selected date");
        }
        
        return checkNext(booking, account);
    }
}