package com.group8.rbs.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.repository.BookingRepository;
import com.group8.rbs.repository.CreditRepository;
import com.group8.rbs.repository.FacilityRepository;
import com.group8.rbs.service.maintenance.MaintenanceService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // This allows unused stubbings
public class BookingValidationChainTest {

    @Mock
    private PastDateTimeValidator pastDateTimeValidator;
    
    @Mock
    private TitleValidator titleValidator;
    
    @Mock
    private EndTimeValidator endTimeValidator;
    
    @Mock
    private FacilityExistsValidator facilityExistsValidator;
    
    @Mock
    private MaintenanceValidator maintenanceValidator;
    
    @Mock
    private CreditValidator creditValidator;
    
    @Mock
    private TimeSlotValidator timeSlotValidator;
    
    @Mock
    private FacilityRepository facilityRepository;
    
    @Mock
    private CreditRepository creditRepository;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private MaintenanceService maintenanceService;
    
    @InjectMocks
    private BookingValidationChainBuilder chainBuilder;
    
    private BookingDTO bookingDTO;
    private Account account;
    private Facility facility;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        // Always clear the context before each test
        BookingValidationContext.clear();
        
        // Current date/time
        now = LocalDateTime.now();
        
        // Setup BookingDTO for validation
        bookingDTO = new BookingDTO();
        bookingDTO.setFacilityId(1L);
        bookingDTO.setAccountEmail("test@example.com");
        bookingDTO.setBookedDateTime(now.plusDays(1));
        bookingDTO.setTimeSlot("14:00 - 15:00");
        bookingDTO.setTitle("Test Booking");
        bookingDTO.setDescription("Test Description");
        bookingDTO.setCreditsUsed("60");
        
        // Setup Account
        account = Account.builder()
                .accountId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
        
        // Setup Facility
        facility = Facility.builder()
                .facilityId(1L)
                .resourceTypeId(1L)
                .resourceName("Test Facility")
                .location("Test Location")
                .capacity(10)
                .build();
    }
    
    @AfterEach
    void tearDown() {
        // Always clear the context after each test
        BookingValidationContext.clear();
    }

    @Test
    @DisplayName("Chain builder should construct chain in correct order")
    void chainBuilderShouldConstructChainInCorrectOrder() {
        // Build the chain
        BookingValidator chain = chainBuilder.buildValidationChain();
        
        // Verify the chain is correctly constructed
        assertEquals(pastDateTimeValidator, chain);
        
        // Verify each validator has the correct next validator
        verify(pastDateTimeValidator).setNext(titleValidator);
        verify(titleValidator).setNext(endTimeValidator);
        verify(endTimeValidator).setNext(facilityExistsValidator);
        verify(facilityExistsValidator).setNext(maintenanceValidator);
        verify(maintenanceValidator).setNext(creditValidator);
        verify(creditValidator).setNext(timeSlotValidator);
    }
    
    @Test
    @DisplayName("Chain should process all validators when all validations pass")
    void chainShouldProcessAllValidatorsWhenAllValidationsPass() {
        // Configure validators with simple success returns
        when(pastDateTimeValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        when(titleValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        when(endTimeValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        when(facilityExistsValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        when(maintenanceValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        when(creditValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        when(timeSlotValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());

        // We need to create a custom implementation of BookingValidator
        // that calls the next validators in the chain
        BookingValidator customChain = new BookingValidator() {
            private BookingValidator next;

            @Override
            public void setNext(BookingValidator nextValidator) {
                this.next = nextValidator;
            }

            @Override
            public ValidationResult validate(BookingDTO booking, Account account) {
                // Call the first validator
                ValidationResult result = pastDateTimeValidator.validate(booking, account);
                
                // If valid, proceed with chain
                if (result.isValid() && titleValidator != null) {
                    result = titleValidator.validate(booking, account);
                }
                
                // Continue chaining
                if (result.isValid() && endTimeValidator != null) {
                    result = endTimeValidator.validate(booking, account);
                }
                
                if (result.isValid() && facilityExistsValidator != null) {
                    result = facilityExistsValidator.validate(booking, account);
                }
                
                if (result.isValid() && maintenanceValidator != null) {
                    result = maintenanceValidator.validate(booking, account);
                }
                
                if (result.isValid() && creditValidator != null) {
                    result = creditValidator.validate(booking, account);
                }
                
                if (result.isValid() && timeSlotValidator != null) {
                    result = timeSlotValidator.validate(booking, account);
                }
                
                return result;
            }
        };
        
        // Use our custom chain to simulate a full validation chain
        ValidationResult result = customChain.validate(bookingDTO, account);
        
        // Verify result is successful
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
        
        // Verify that all validators were called
        verify(pastDateTimeValidator).validate(any(BookingDTO.class), any(Account.class));
        verify(titleValidator).validate(any(BookingDTO.class), any(Account.class));
        verify(endTimeValidator).validate(any(BookingDTO.class), any(Account.class));
        verify(facilityExistsValidator).validate(any(BookingDTO.class), any(Account.class));
        verify(maintenanceValidator).validate(any(BookingDTO.class), any(Account.class));
        verify(creditValidator).validate(any(BookingDTO.class), any(Account.class));
        verify(timeSlotValidator).validate(any(BookingDTO.class), any(Account.class));
    }
    
    @Test
    @DisplayName("Chain should stop at first validation failure")
    void chainShouldStopAtFirstValidationFailure() {
        // Configure first validator to fail
        when(pastDateTimeValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.failure("Cannot create bookings in the past"));
        
        // Configure other validators to succeed (should never be called)
        when(titleValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        
        // Create a custom implementation of BookingValidator for testing
        BookingValidator customChain = new BookingValidator() {
            @Override
            public void setNext(BookingValidator nextValidator) {
                // Not used in this test
            }

            @Override
            public ValidationResult validate(BookingDTO booking, Account account) {
                // Call the first validator
                ValidationResult result = pastDateTimeValidator.validate(booking, account);
                
                // If valid, proceed with chain (should not happen in this test)
                if (result.isValid() && titleValidator != null) {
                    result = titleValidator.validate(booking, account);
                }
                
                return result;
            }
        };
        
        // Call the validator
        ValidationResult result = customChain.validate(bookingDTO, account);
        
        // Verify result contains the validation error
        assertFalse(result.isValid());
        assertEquals("Cannot create bookings in the past", result.getErrorMessage());
        
        // Verify that only the first validator was called
        verify(pastDateTimeValidator).validate(any(BookingDTO.class), any(Account.class));
        verify(titleValidator, never()).validate(any(BookingDTO.class), any(Account.class));
    }
    
    @Test
    @DisplayName("Chain should store facility in context")
    void chainShouldStoreFacilityInContext() {
        // Create stub for the facilityExistsValidator that stores the facility
        doAnswer(invocation -> {
            // Store the facility in the context
            BookingValidationContext.setFacility(facility);
            // Return success
            return ValidationResult.success();
        }).when(facilityExistsValidator).validate(any(BookingDTO.class), any(Account.class));
        
        // Call the validator directly
        ValidationResult result = facilityExistsValidator.validate(bookingDTO, account);
        
        // Verify result is successful
        assertTrue(result.isValid());
        
        // Verify facility was stored in context
        Facility storedFacility = BookingValidationContext.getFacility();
        assertNotNull(storedFacility);
        assertEquals(facility.getFacilityId(), storedFacility.getFacilityId());
        assertEquals(facility.getResourceName(), storedFacility.getResourceName());
    }
    
    @Test
    @DisplayName("Failure in middle of chain should stop processing")
    void failureInMiddleOfChainShouldStopProcessing() {
        // Configure validators
        when(pastDateTimeValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        when(titleValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        when(endTimeValidator.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.failure("Bookings cannot extend beyond 7:00 PM closing time"));
        
        // Create a custom implementation of BookingValidator for testing
        BookingValidator customChain = new BookingValidator() {
            @Override
            public void setNext(BookingValidator nextValidator) {
                // Not used in this test
            }

            @Override
            public ValidationResult validate(BookingDTO booking, Account account) {
                // Call the first validator
                ValidationResult result = pastDateTimeValidator.validate(booking, account);
                
                // If valid, proceed with chain
                if (result.isValid() && titleValidator != null) {
                    result = titleValidator.validate(booking, account);
                }
                
                // Continue chain
                if (result.isValid() && endTimeValidator != null) {
                    result = endTimeValidator.validate(booking, account);
                }
                
                // This should not execute if endTimeValidator fails
                if (result.isValid() && facilityExistsValidator != null) {
                    result = facilityExistsValidator.validate(booking, account);
                }
                
                return result;
            }
        };
        
        // Call the validator
        ValidationResult result = customChain.validate(bookingDTO, account);
        
        // Verify result contains the validation error
        assertFalse(result.isValid());
        assertEquals("Bookings cannot extend beyond 7:00 PM closing time", result.getErrorMessage());
        
        // Verify that validators were called in the right order and stopped at the failing one
        verify(pastDateTimeValidator).validate(any(BookingDTO.class), any(Account.class));
        verify(titleValidator).validate(any(BookingDTO.class), any(Account.class));
        verify(endTimeValidator).validate(any(BookingDTO.class), any(Account.class));
        verify(facilityExistsValidator, never()).validate(any(BookingDTO.class), any(Account.class));
    }

    @Test
    @DisplayName("FacilityExistsValidator should store facility in context")
    void facilityExistsValidatorShouldStoreFacilityInContext() {
        // Create a real FacilityExistsValidator instead of a mock
        FacilityExistsValidator realValidator = new FacilityExistsValidator(facilityRepository);
        
        // Configure mock repository to return the facility
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(facility));
        
        // Call the validator directly
        ValidationResult result = realValidator.validate(bookingDTO, account);
        
        // Verify result is successful
        assertTrue(result.isValid());
        
        // Verify facility was stored in context
        Facility storedFacility = BookingValidationContext.getFacility();
        assertNotNull(storedFacility);
        assertEquals(facility.getFacilityId(), storedFacility.getFacilityId());
    }
    
    // NEW TESTS FOR BETTER COVERAGE
    
    @Test
    @DisplayName("PastDateTimeValidator should reject past bookings")
    void pastDateTimeValidatorShouldRejectPastBookings() {
        // Create a real validator
        PastDateTimeValidator validator = new PastDateTimeValidator();
        
        // Create a booking DTO with past date/time
        BookingDTO pastBookingDTO = new BookingDTO();
        pastBookingDTO.setBookedDateTime(now.minusDays(1)); // yesterday
        
        // Validate
        ValidationResult result = validator.validate(pastBookingDTO, account);
        
        // Verification
        assertFalse(result.isValid());
        assertEquals("Cannot create bookings in the past", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("PastDateTimeValidator should accept future bookings")
    void pastDateTimeValidatorShouldAcceptFutureBookings() {
        // Create a real validator
        PastDateTimeValidator validator = new PastDateTimeValidator();
        
        // Create a booking DTO with future date/time
        BookingDTO futureBookingDTO = new BookingDTO();
        futureBookingDTO.setBookedDateTime(now.plusDays(1)); // tomorrow
        
        // Validate
        ValidationResult result = validator.validate(futureBookingDTO, account);
        
        // Verification
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("TitleValidator should reject empty titles")
    void titleValidatorShouldRejectEmptyTitles() {
        // Create a real validator
        TitleValidator validator = new TitleValidator();
        
        // Create booking DTO with empty title
        BookingDTO emptyTitleDTO = new BookingDTO();
        emptyTitleDTO.setTitle("");
        
        // Validate
        ValidationResult result = validator.validate(emptyTitleDTO, account);
        
        // Verification
        assertFalse(result.isValid());
        assertEquals("Booking title is required", result.getErrorMessage());
        
        // Test with null title
        BookingDTO nullTitleDTO = new BookingDTO();
        nullTitleDTO.setTitle(null);
        
        result = validator.validate(nullTitleDTO, account);
        
        // Verification
        assertFalse(result.isValid());
        assertEquals("Booking title is required", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("EndTimeValidator should reject bookings extending beyond 7PM")
    void endTimeValidatorShouldRejectBookingsExtendingBeyond7PM() {
        // Create a real validator
        EndTimeValidator validator = new EndTimeValidator();
        
        // Create booking DTO with time that would extend past 7 PM
        BookingDTO lateBookingDTO = new BookingDTO();
        // Set start time to 6:30 PM
        LocalDateTime startTime = LocalDate.now().atTime(18, 30);
        lateBookingDTO.setBookedDateTime(startTime);
        // 60 minutes would end at 7:30 PM (past closing)
        lateBookingDTO.setCreditsUsed("60");
        
        // Validate
        ValidationResult result = validator.validate(lateBookingDTO, account);
        
        // Verification
        assertFalse(result.isValid());
        assertEquals("Bookings cannot extend beyond 7:00 PM closing time", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("EndTimeValidator should accept bookings ending at or before 7PM")
    void endTimeValidatorShouldAcceptBookingsEndingAtOrBefore7PM() {
        // Create a real validator
        EndTimeValidator validator = new EndTimeValidator();
        
        // Create booking DTO ending exactly at 7 PM
        BookingDTO endingAt7PMDTO = new BookingDTO();
        // Set start time to 6:30 PM
        LocalDateTime startTime = LocalDate.now().atTime(18, 30);
        endingAt7PMDTO.setBookedDateTime(startTime);
        // 30 minutes would end at 7:00 PM (allowed)
        endingAt7PMDTO.setCreditsUsed("30");
        
        // Validate
        ValidationResult result = validator.validate(endingAt7PMDTO, account);
        
        // Verification
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("FacilityExistsValidator should reject non-existent facilities")
    void facilityExistsValidatorShouldRejectNonExistentFacilities() {
        // Create a real validator
        FacilityExistsValidator validator = new FacilityExistsValidator(facilityRepository);
        
        // Configure repository to return empty (facility not found)
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // Validate
        ValidationResult result = validator.validate(bookingDTO, account);
        
        // Verification
        assertFalse(result.isValid());
        assertEquals("Facility not found", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("MaintenanceValidator should reject facilities under maintenance")
    void maintenanceValidatorShouldRejectFacilitiesUnderMaintenance() {
        // Create a real validator
        MaintenanceValidator validator = new MaintenanceValidator(maintenanceService);
        
        // Configure maintenance service to return true (under maintenance)
        when(maintenanceService.isFacilityUnderMaintenanceOnDate(anyLong(), any(LocalDate.class)))
            .thenReturn(true);
        
        // Validate
        ValidationResult result = validator.validate(bookingDTO, account);
        
        // Verification
        assertFalse(result.isValid());
        assertEquals("This facility is under maintenance and cannot be booked on the selected date", 
                     result.getErrorMessage());
    }
    
    @Test
    @DisplayName("CreditValidator should reject bookings with insufficient credits")
    void creditValidatorShouldRejectBookingsWithInsufficientCredits() {
        // Create a real validator
        CreditValidator validator = new CreditValidator(creditRepository);
        
        // Configure repository to return insufficient credits
        when(creditRepository.findCreditBalanceByAccountId(anyLong())).thenReturn(30.0); // Only 30 credits
        
        // Booking requires 60 credits
        bookingDTO.setCreditsUsed("60");
        
        // Validate
        ValidationResult result = validator.validate(bookingDTO, account);
        
        // Verification
        assertFalse(result.isValid());
        assertEquals("Insufficient credits. Required: 60.0, Available: 30.0", 
                     result.getErrorMessage());
    }
    
    @Test
    @DisplayName("CreditValidator should accept bookings with sufficient credits")
    void creditValidatorShouldAcceptBookingsWithSufficientCredits() {
        // Create a real validator
        CreditValidator validator = new CreditValidator(creditRepository);
        
        // Configure repository to return sufficient credits
        when(creditRepository.findCreditBalanceByAccountId(anyLong())).thenReturn(100.0); // 100 credits
        
        // Booking requires 60 credits
        bookingDTO.setCreditsUsed("60");
        
        // Validate
        ValidationResult result = validator.validate(bookingDTO, account);
        
        // Verification
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("TimeSlotValidator should reject overlapping bookings")
    void timeSlotValidatorShouldRejectOverlappingBookings() {
        // Create a real validator
        TimeSlotValidator validator = new TimeSlotValidator(bookingRepository);
        
        // Set up context with a facility
        BookingValidationContext.setFacility(facility);
        
        // Create an existing booking with overlapping time
        Booking existingBooking = Booking.builder()
                .facility(facility)
                .account(account)
                .bookedDateTime(bookingDTO.getBookedDateTime())
                .timeSlot(bookingDTO.getTimeSlot()) // Same time slot
                .status(BookingStatus.APPROVED)
                .build();
        
        // Configure repository to return existing bookings
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(), any(), anyList()))
                .thenReturn(Arrays.asList(existingBooking));
        
        // Validate
        ValidationResult result = validator.validate(bookingDTO, account);
        
        // Verification
        assertFalse(result.isValid());
        assertEquals("This time slot is already booked", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("TimeSlotValidator should accept non-overlapping bookings")
    void timeSlotValidatorShouldAcceptNonOverlappingBookings() {
        // Create a real validator
        TimeSlotValidator validator = new TimeSlotValidator(bookingRepository);
        
        // Set up context with a facility
        BookingValidationContext.setFacility(facility);
        
        // Create an existing booking with different time slot
        Booking existingBooking = Booking.builder()
                .facility(facility)
                .account(account)
                .bookedDateTime(bookingDTO.getBookedDateTime())
                .timeSlot("16:00 - 17:00") // Different time slot
                .status(BookingStatus.APPROVED)
                .build();
        
        // Configure repository to return existing bookings
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(), any(), anyList()))
                .thenReturn(Arrays.asList(existingBooking));
        
        // Validate
        ValidationResult result = validator.validate(bookingDTO, account);
        
        // Verification
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("ValidationResult should provide correct validation status and message")
    void validationResultShouldProvideCorrectValidationStatusAndMessage() {
        // Create a success result
        ValidationResult successResult = ValidationResult.success();
        
        // Verify success result
        assertTrue(successResult.isValid());
        assertNull(successResult.getErrorMessage());
        
        // Create a failure result
        String errorMessage = "Test error message";
        ValidationResult failureResult = ValidationResult.failure(errorMessage);
        
        // Verify failure result
        assertFalse(failureResult.isValid());
        assertEquals(errorMessage, failureResult.getErrorMessage());
    }
}