package com.group8.rbs.service.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.dto.facility.FacilityNameOptionsResponse;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.enums.AccountType;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.mapper.BookingFacilityMapper;
import com.group8.rbs.mapper.BookingMapper;
import com.group8.rbs.repository.AccountRepository;
import com.group8.rbs.repository.BookingRepository;
import com.group8.rbs.repository.CreditRepository;
import com.group8.rbs.repository.FacilityRepository;
import com.group8.rbs.repository.FacilityTypeRepository;
import com.group8.rbs.validation.BookingValidationChainBuilder;
import com.group8.rbs.validation.BookingValidationContext;
import com.group8.rbs.validation.BookingValidator;
import com.group8.rbs.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Allow unused stubbings
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private CreditRepository creditRepository;
    
    @Mock
    private FacilityTypeRepository facilityTypeRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
private BookingWebSocketService bookingWebSocketService;        


    @Mock
    private BookingFacilityMapper bookingFacilityMapper;
    
    @Mock
    private BookingValidationChainBuilder validationChainBuilder;
    
    @Mock
    private BookingValidator validationChain;
    
    // Not using @InjectMocks - we'll manually create the service to ensure proper chain initialization
    private BookingService bookingService;

    private BookingDTO bookingDTO;
    private Booking booking;
    private BookingResponseDTO bookingResponseDTO;
    private Account account;
    private Facility facility;
    private FacilitySearchDTO searchDTO;
    private List<Facility> facilities;
    private List<FacilitySearchDTO> facilitySearchResults;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        // Current date/time
        now = LocalDateTime.now();
        
        // Setup Account
        account = Account.builder()
                .accountId(1L)
                .email("test@example.com")
                .name("Test Student")
                .accountType(AccountType.STUDENT)
                .build();
        
        // Setup Facility
        facility = Facility.builder()
                .facilityId(1L)
                .resourceTypeId(1L)
                .resourceName("Test Facility")
                .location("Test Location")
                .capacity(10)
                .bookings(new ArrayList<>()) // Initialize with empty list
                .build();
        
        // Setup Booking
        booking = Booking.builder()
                .bookingId(1L)
                .account(account)
                .facility(facility)
                .bookedDateTime(now.plusDays(1))
                .timeSlot("14:00 - 15:00")
                .title("Test Booking")
                .description("Test Description")
                .status(BookingStatus.APPROVED)
                .build();
        
        // Setup BookingDTO
        bookingDTO = new BookingDTO();
        bookingDTO.setFacilityId(1L);
        bookingDTO.setAccountEmail("test@example.com");
        bookingDTO.setBookedDateTime(now.plusDays(1));
        bookingDTO.setTimeSlot("14:00 - 15:00");
        bookingDTO.setTitle("Test Booking");
        bookingDTO.setDescription("Test Description");
        bookingDTO.setCreditsUsed("60");
        
        // Setup BookingResponseDTO
        bookingResponseDTO = BookingResponseDTO.builder()
                .bookingId(1L)
                .studentId("1")
                .studentName("Test Student")
                .facilityName("Test Facility")
                .location("Test Location")
                .bookedDatetime(now.plusDays(1))
                .timeslot("14:00 - 15:00")
                .status("APPROVED")
                .email("test@example.com")
                .title("Test Booking")
                .description("Test Description")
                .build();
        
        // Setup Search DTO and results
        searchDTO = FacilitySearchDTO.builder()
                .resourceTypeId(1L)
                .resourceName("Test Facility")
                .location("Test Location")
                .capacity(10)
                .date(LocalDate.now())
                .build();
        
        facilities = Arrays.asList(facility);
        
        facilitySearchResults = Arrays.asList(
            FacilitySearchDTO.builder()
                .facilityId(1L)
                .resourceTypeId(1L)
                .resourceName("Test Facility")
                .location("Test Location")
                .capacity(10)
                .build()
        );
        
        // IMPORTANT: Set up the validation chain
        when(validationChainBuilder.buildValidationChain()).thenReturn(validationChain);
        
        // Manually create the service instead of using @InjectMocks
        // This ensures the validationChain is properly set up
        bookingService = new BookingService(
            bookingRepository,
            bookingMapper,
            facilityTypeRepository,
            facilityRepository,
            bookingFacilityMapper,
            accountRepository,
            creditRepository,
            validationChainBuilder
        );
    }

    @Test
    @DisplayName("Search facilities should return filtered facilities")
    void searchFacilitiesShouldReturnFilteredFacilities() {
        when(facilityRepository.searchFacilities(any(), any(), any(), any())).thenReturn(facilities);
        when(bookingFacilityMapper.toResponseDTO(any(Facility.class))).thenReturn(facilitySearchResults.get(0));
        
        List<FacilitySearchDTO> result = bookingService.searchFacilities(searchDTO);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Test Facility", result.get(0).getResourceName());
        assertEquals("Test Location", result.get(0).getLocation());
        verify(facilityRepository).searchFacilities(any(), any(), any(), any());
        verify(bookingFacilityMapper).toResponseDTO(any(Facility.class));
    }
    
    @Test
    @DisplayName("Search facilities with null criteria should handle it correctly")
    void searchFacilitiesWithNullCriteriaShouldHandleItCorrectly() {
        // Create search criteria with null values
        FacilitySearchDTO nullSearchDTO = FacilitySearchDTO.builder()
                .resourceTypeId(null)
                .resourceName(null)
                .location(null)
                .capacity(null)
                .date(LocalDate.now())
                .build();
                
        when(facilityRepository.searchFacilities(null, null, null, null)).thenReturn(facilities);
        when(bookingFacilityMapper.toResponseDTO(any(Facility.class))).thenReturn(facilitySearchResults.get(0));
        
        List<FacilitySearchDTO> result = bookingService.searchFacilities(nullSearchDTO);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(facilityRepository).searchFacilities(null, null, null, null);
    }
    
    @Test
    @DisplayName("Search facilities with date filter should filter bookings by date")
    void searchFacilitiesWithDateFilterShouldFilterBookingsByDate() {
        // Create facility with bookings
        LocalDate today = LocalDate.now();
        
        Booking booking1 = Booking.builder()
                .bookingId(1L)
                .facility(facility)
                .account(account)
                .bookedDateTime(today.atTime(10, 0))
                .timeSlot("10:00 - 11:00")
                .status(BookingStatus.APPROVED)
                .build();
                
        Booking booking2 = Booking.builder()
                .bookingId(2L)
                .facility(facility)
                .account(account)
                .bookedDateTime(today.plusDays(1).atTime(10, 0))
                .timeSlot("10:00 - 11:00")
                .status(BookingStatus.APPROVED)
                .build();
                
        facility.setBookings(Arrays.asList(booking1, booking2));
        
        when(facilityRepository.searchFacilities(any(), any(), any(), any())).thenReturn(Arrays.asList(facility));
        when(bookingFacilityMapper.toResponseDTO(any(Facility.class))).thenReturn(facilitySearchResults.get(0));
        
        List<FacilitySearchDTO> result = bookingService.searchFacilities(searchDTO);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(facilityRepository).searchFacilities(any(), any(), any(), any());
        verify(bookingFacilityMapper).toResponseDTO(any(Facility.class));
    }

    @Test
    @DisplayName("Get dropdown options should return all options")
    void getDropdownOptionsShouldReturnAllOptions() {
        // Mock resource types
        List<Object[]> resourceTypeResults = Collections.singletonList(new Object[]{ 1L, "Meeting Room" });
        when(facilityTypeRepository.findAllFacilityTypeOptions()).thenReturn(resourceTypeResults);
        
        // Mock locations
        List<String> locations = Arrays.asList("Building A", "Building B");
        when(facilityRepository.findAllLocations()).thenReturn(locations);
        
        // Mock resource names
        List<String> resourceNames = Arrays.asList("Room 1", "Room 2");
        when(facilityRepository.findAllResourceNames()).thenReturn(resourceNames);
        
        var options = bookingService.getDropdownOptions();
        
        assertNotNull(options);
        assertTrue(options.containsKey("resourceTypes"));
        assertTrue(options.containsKey("locations"));
        assertTrue(options.containsKey("resourceNames"));
        
        // Further check the content of resourceTypes
        Object resourceTypes = options.get("resourceTypes");
        assertTrue(resourceTypes instanceof List);
        assertFalse(((List<?>) resourceTypes).isEmpty());
        
        // Verify all repository methods were called
        verify(facilityTypeRepository).findAllFacilityTypeOptions();
        verify(facilityRepository).findAllLocations();
        verify(facilityRepository).findAllResourceNames();
    }
    
    @Test
    @DisplayName("Get resource types should return mapped facility type options")
    void getResourceTypesShouldReturnMappedFacilityTypeOptions() {
        // Mock resource types
        List<Object[]> resourceTypeResults = Arrays.asList(
            new Object[]{ 1L, "Meeting Room" },
            new Object[]{ 2L, "Study Room" }
        );
        when(facilityTypeRepository.findAllFacilityTypeOptions()).thenReturn(resourceTypeResults);
        
        List<FacilityNameOptionsResponse> result = bookingService.getResourceTypes();
        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getFacilityTypeId());
        assertEquals("Meeting Room", result.get(0).getFacilityType());
        assertEquals(2L, result.get(1).getFacilityTypeId());
        assertEquals("Study Room", result.get(1).getFacilityType());
    }

    @Test
    @DisplayName("Create booking should use validation chain")
    void createBookingShouldUseValidationChain() {
        // Mock account repository
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        
        // Mock validation chain - successful validation
        when(validationChain.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        
        // Store facility in validation context
        BookingValidationContext.setFacility(facility);
        
        // Mock credit repository
        when(creditRepository.checkAndDeductCredits(anyLong(), anyDouble())).thenReturn(1);
        
        // Mock booking repository
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        
        // Mock booking mapper
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(bookingResponseDTO);
        
        try {
            // Call method under test
            BookingResponseDTO result = bookingService.createBooking(bookingDTO);
            
            // Verify validation chain was used
            verify(validationChainBuilder).buildValidationChain();
            verify(validationChain).validate(eq(bookingDTO), eq(account));
            
            // Verify result
            assertNotNull(result);
            assertEquals("Test Facility", result.getFacilityName());
        } finally {
            // Clean up context even if test fails
            BookingValidationContext.clear();
        }
    }
    
    @Test
    @DisplayName("Create booking should throw exception when validation fails")
    void createBookingShouldThrowExceptionWhenValidationFails() {
        // Mock account repository
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        
        // Mock validation chain - failed validation
        when(validationChain.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.failure("Validation failed"));
        
        // Call method and verify exception
        Exception exception = assertThrows(RuntimeException.class, 
                () -> bookingService.createBooking(bookingDTO));
        
        assertEquals("Validation failed", exception.getMessage());
        
        // Verify validation chain was used
        verify(validationChainBuilder).buildValidationChain();
        verify(validationChain).validate(eq(bookingDTO), eq(account));
        
        // Verify booking was not saved
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(creditRepository, never()).checkAndDeductCredits(anyLong(), anyDouble());
    }
    
    @Test
    @DisplayName("Create booking should throw exception when account not found")
    void createBookingShouldThrowExceptionWhenAccountNotFound() {
        // Mock account repository to return empty
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        // Call method and verify exception
        Exception exception = assertThrows(RuntimeException.class, 
                () -> bookingService.createBooking(bookingDTO));
        
        assertEquals("Account not found", exception.getMessage());
        
        // Verify validation chain was not used
        verify(validationChain, never()).validate(any(BookingDTO.class), any(Account.class));
        
        // Verify booking was not saved
        verify(bookingRepository, never()).save(any(Booking.class));
    }
    
    @Test
    @DisplayName("Create booking with sports facility should set PENDING status")
    void createBookingWithSportsFacilityShouldSetPendingStatus() {
        // Setup - facility with sports type
        Facility sportsFacility = Facility.builder()
                .facilityId(1L)
                .resourceTypeId(5L) // Sports facility type ID
                .resourceName("Sports Facility")
                .build();
        
        try {
            // Mock validation context
            BookingValidationContext.setFacility(sportsFacility);
            
            // Mock account repository
            when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
            
            // Mock validation chain - successful validation
            when(validationChain.validate(any(BookingDTO.class), any(Account.class)))
                .thenReturn(ValidationResult.success());
            
            // Mock credit repository
            when(creditRepository.checkAndDeductCredits(anyLong(), anyDouble())).thenReturn(1);
            
            // Create a pending booking for the result
            Booking pendingBooking = Booking.builder()
                    .bookingId(1L)
                    .status(BookingStatus.PENDING)
                    .build();
            
            // Mock booking repository
            when(bookingRepository.save(any(Booking.class))).thenReturn(pendingBooking);
            
            // Mock response DTO with PENDING status
            BookingResponseDTO pendingResponseDTO = BookingResponseDTO.builder()
                    .bookingId(1L)
                    .status("PENDING")
                    .build();
                    
            when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(pendingResponseDTO);
            
            // Call method under test
            BookingResponseDTO result = bookingService.createBooking(bookingDTO);
            
            // Verify status is PENDING
            assertEquals("PENDING", result.getStatus());
            
            // Verify correct status was set in booking entity
            ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(bookingCaptor.capture());
            assertEquals(BookingStatus.PENDING, bookingCaptor.getValue().getStatus());
        } finally {
            // Clean up context even if test fails
            BookingValidationContext.clear();
        }
    }
    
    @Test
    @DisplayName("Create booking with non-sports facility should set APPROVED status")
    void createBookingWithNonSportsFacilityShouldSetApprovedStatus() {
        // Setup - facility with meeting room type
        Facility meetingFacility = Facility.builder()
                .facilityId(1L)
                .resourceTypeId(1L) // Non-sports facility type
                .resourceName("Meeting Room")
                .build();
        
        try {
            // Mock validation context
            BookingValidationContext.setFacility(meetingFacility);
            
            // Mock account repository
            when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
            
            // Mock validation chain - successful validation
            when(validationChain.validate(any(BookingDTO.class), any(Account.class)))
                .thenReturn(ValidationResult.success());
            
            // Mock credit repository
            when(creditRepository.checkAndDeductCredits(anyLong(), anyDouble())).thenReturn(1);
            
            // Create an approved booking for the result
            Booking approvedBooking = Booking.builder()
                    .bookingId(1L)
                    .status(BookingStatus.APPROVED)
                    .build();
            
            // Mock booking repository
            when(bookingRepository.save(any(Booking.class))).thenReturn(approvedBooking);
            
            // Mock response DTO with APPROVED status
            BookingResponseDTO approvedResponseDTO = BookingResponseDTO.builder()
                    .bookingId(1L)
                    .status("APPROVED")
                    .build();
                    
            when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(approvedResponseDTO);
            
            // Call method under test
            BookingResponseDTO result = bookingService.createBooking(bookingDTO);
            
            // Verify status is APPROVED
            assertEquals("APPROVED", result.getStatus());
            
            // Verify correct status was set in booking entity
            ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(bookingCaptor.capture());
            assertEquals(BookingStatus.APPROVED, bookingCaptor.getValue().getStatus());
        } finally {
            // Clean up context even if test fails
            BookingValidationContext.clear();
        }
    }

    @Test
    @DisplayName("Create booking should clean up context after exception")
    void createBookingShouldCleanUpContextAfterException() {
        // Setup - set facility in context
        BookingValidationContext.setFacility(facility);
        
        // Mock account repository
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        
        // Mock validation chain to throw exception
        RuntimeException validationException = new RuntimeException("Validation error");
        when(validationChain.validate(any(BookingDTO.class), any(Account.class)))
            .thenThrow(validationException);
        
        // Call method and verify exception
        Exception exception = assertThrows(RuntimeException.class, 
                () -> bookingService.createBooking(bookingDTO));
        
        // Make sure it's the exact same exception object we created
        assertSame(validationException, exception);
        assertEquals("Validation error", exception.getMessage());
        
        // Verify context is cleared even after exception
        assertNull(BookingValidationContext.getFacility());
    }
    
    @Test
    @DisplayName("Get upcoming approved or confirmed bookings should return bookings")
    void getUpcomingApprovedOrConfirmedBookingsShouldReturnBookings() {
        when(bookingRepository.findUpcomingApprovedOrConfirmedBookings(
                anyLong(), anyList(), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(booking));
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(bookingResponseDTO);
        
        List<BookingResponseDTO> result = bookingService.getUpcomingApprovedOrConfirmedBookings(1L);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("APPROVED", result.get(0).getStatus());
        verify(bookingRepository).findUpcomingApprovedOrConfirmedBookings(
                anyLong(), anyList(), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Get pending future bookings should return bookings")
    void getPendingFutureBookingsShouldReturnBookings() {
        when(bookingRepository.findByAccount_AccountIdAndStatusAndBookedDateTimeAfter(
                anyLong(), any(BookingStatus.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(booking));
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(bookingResponseDTO);
        
        List<BookingResponseDTO> result = bookingService.getPendingFutureBookings(1L);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(bookingRepository).findByAccount_AccountIdAndStatusAndBookedDateTimeAfter(
                anyLong(), any(BookingStatus.class), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Get booking history should return bookings with no status")
    void getBookingHistoryShouldReturnBookingsWithNoStatus() {
        when(bookingRepository.findByAccount_AccountIdAndBookedDateTimeBefore(
                anyLong(), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(booking));
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(bookingResponseDTO);
        
        List<BookingResponseDTO> result = bookingService.getBookingHistory(1L, null);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(bookingRepository).findByAccount_AccountIdAndBookedDateTimeBefore(
                anyLong(), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Get booking history should return bookings with status")
    void getBookingHistoryShouldReturnBookingsWithStatus() {
        when(bookingRepository.findByAccount_AccountIdAndStatusAndBookedDateTimeBefore(
                anyLong(), any(BookingStatus.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(booking));
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(bookingResponseDTO);
        
        List<BookingResponseDTO> result = bookingService.getBookingHistory(1L, "APPROVED");
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(bookingRepository).findByAccount_AccountIdAndStatusAndBookedDateTimeBefore(
                anyLong(), any(BookingStatus.class), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Delete booking should return true when successful")
    void deleteBookingShouldReturnTrueWhenSuccessful() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        
        boolean result = bookingService.deleteBooking(1L);
        
        assertTrue(result);
        verify(bookingRepository).delete(any(Booking.class));
    }
    
    @Test
    @DisplayName("Delete booking should return false when booking not found")
    void deleteBookingShouldReturnFalseWhenBookingNotFound() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        boolean result = bookingService.deleteBooking(1L);
        
        assertFalse(result);
        verify(bookingRepository, never()).delete(any(Booking.class));
    }
    
    @Test
    @DisplayName("Update booking status should return true when successful")
    void updateBookingStatusShouldReturnTrueWhenSuccessful() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        
        boolean result = bookingService.updateBookingStatus(1L, BookingStatus.CONFIRMED);
        
        assertTrue(result);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository).save(any(Booking.class));
    }
    
    @Test
    @DisplayName("Update booking status should return false when booking not found")
    void updateBookingStatusShouldReturnFalseWhenBookingNotFound() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        boolean result = bookingService.updateBookingStatus(1L, BookingStatus.CONFIRMED);
        
        assertFalse(result);
        verify(bookingRepository, never()).save(any(Booking.class));
    }
    
    @Test
    @DisplayName("Get bookings by status should return bookings")
    void getBookingsByStatusShouldReturnBookings() {
        when(bookingRepository.findByStatus(any(BookingStatus.class)))
                .thenReturn(Arrays.asList(booking));
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(bookingResponseDTO);
        
        List<BookingResponseDTO> result = bookingService.getBookingsByStatus("APPROVED");
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(bookingRepository).findByStatus(any(BookingStatus.class));
    }
    
    // NEW TESTS FOR BETTER COVERAGE
    
    @Test
    @DisplayName("Get bookings by status should return empty list when no bookings found")
    void getBookingsByStatusShouldReturnEmptyListWhenNoBookingsFound() {
        when(bookingRepository.findByStatus(any(BookingStatus.class)))
                .thenReturn(Collections.emptyList());
        
        List<BookingResponseDTO> result = bookingService.getBookingsByStatus("PENDING");
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookingRepository).findByStatus(BookingStatus.PENDING);
    }
    
    @Test
    @DisplayName("Get locations should return all distinct locations")
    void getLocationsShouldReturnAllDistinctLocations() {
        List<String> expectedLocations = Arrays.asList("Building A", "Building B", "Building C");
        when(facilityRepository.findAllLocations()).thenReturn(expectedLocations);
        
        List<String> result = bookingService.getLocations();
        
        assertNotNull(result);
        assertEquals(expectedLocations.size(), result.size());
        assertEquals(expectedLocations, result);
        verify(facilityRepository).findAllLocations();
    }
    
    @Test
    @DisplayName("Get resource names should return all distinct resource names")
    void getResourceNamesShouldReturnAllDistinctResourceNames() {
        List<String> expectedNames = Arrays.asList("Room 101", "Lab 202", "Hall 303");
        when(facilityRepository.findAllResourceNames()).thenReturn(expectedNames);
        
        List<String> result = bookingService.getResourceNames();
        
        assertNotNull(result);
        assertEquals(expectedNames.size(), result.size());
        assertEquals(expectedNames, result);
        verify(facilityRepository).findAllResourceNames();
    }
    
    @Test
    @DisplayName("Create booking should throw exception when credit deduction fails")
    void createBookingShouldThrowExceptionWhenCreditDeductionFails() {
        // Mock account repository
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        
        // Mock validation chain - successful validation
        when(validationChain.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        
        // Store facility in validation context
        BookingValidationContext.setFacility(facility);
        
        // Mock credit repository to fail the credit deduction (return 0 rows affected)
        when(creditRepository.checkAndDeductCredits(anyLong(), anyDouble())).thenReturn(0);
        
        try {
            // Call method under test and expect exception
            Exception exception = assertThrows(RuntimeException.class, 
                    () -> bookingService.createBooking(bookingDTO));
            
            // Verify exception contains expected message
            assertTrue(exception.getMessage().contains("Insufficient credits"));
            
            // Verify credit repository was called but booking was not saved
            verify(creditRepository).checkAndDeductCredits(eq(1L), eq(60.0));
            verify(bookingRepository, never()).save(any(Booking.class));
        } finally {
            // Clean up context
            BookingValidationContext.clear();
        }
    }
    
    @Test
    @DisplayName("Search facilities should handle empty facility list")
    void searchFacilitiesShouldHandleEmptyFacilityList() {
        // Mock repository to return empty list
        when(facilityRepository.searchFacilities(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        
        List<FacilitySearchDTO> result = bookingService.searchFacilities(searchDTO);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(facilityRepository).searchFacilities(any(), any(), any(), any());
        verify(bookingFacilityMapper, never()).toResponseDTO(any(Facility.class));
    }
    
    @Test
    @DisplayName("Search facilities should filter bookings by date and status")
    void searchFacilitiesShouldFilterBookingsByDateAndStatus() {
        // Create facility with various booking statuses
        LocalDate today = LocalDate.now();
        
        Booking approvedBooking = Booking.builder()
                .bookingId(1L)
                .facility(facility)
                .account(account)
                .bookedDateTime(today.atTime(10, 0))
                .timeSlot("10:00 - 11:00")
                .status(BookingStatus.APPROVED)
                .build();
                
        Booking pendingBooking = Booking.builder()
                .bookingId(2L)
                .facility(facility)
                .account(account)
                .bookedDateTime(today.atTime(12, 0))
                .timeSlot("12:00 - 13:00")
                .status(BookingStatus.PENDING)
                .build();
                
        Booking cancelledBooking = Booking.builder()
                .bookingId(3L)
                .facility(facility)
                .account(account)
                .bookedDateTime(today.atTime(14, 0))
                .timeSlot("14:00 - 15:00")
                .status(BookingStatus.CANCELLED)
                .build();
                
        Booking rejectedBooking = Booking.builder()
                .bookingId(4L)
                .facility(facility)
                .account(account)
                .bookedDateTime(today.atTime(16, 0))
                .timeSlot("16:00 - 17:00")
                .status(BookingStatus.REJECTED)
                .build();
        
        // Set all bookings on the facility
        facility.setBookings(Arrays.asList(
                approvedBooking, pendingBooking, cancelledBooking, rejectedBooking
        ));
        
        when(facilityRepository.searchFacilities(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(facility));
        
        // Mock bookingFacilityMapper to return a simple DTO
        when(bookingFacilityMapper.toResponseDTO(any(Facility.class))).thenReturn(facilitySearchResults.get(0));
        
        // Set the date in the search criteria
        searchDTO.setDate(today);
        
        // Call the search method
        List<FacilitySearchDTO> result = bookingService.searchFacilities(searchDTO);
        
        // Verify result
        assertNotNull(result);
        assertEquals(1, result.size());
        
        // Verify the facility mapper was called with a facility where bookings have been filtered
        verify(bookingFacilityMapper).toResponseDTO(argThat(f -> {
            if (f.getBookings() == null) return false;
            
            // There should be only 2 bookings left after filtering (approved and pending)
            return f.getBookings().size() == 2 && 
                f.getBookings().stream().allMatch(b -> 
                    b.getStatus() == BookingStatus.APPROVED || 
                    b.getStatus() == BookingStatus.PENDING || 
                    b.getStatus() == BookingStatus.CONFIRMED);
        }));
    }
    
    @Test
    @DisplayName("Create booking should use the validated facility from context")
    void createBookingShouldUseTheValidatedFacilityFromContext() {
        // Mock account repository
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        
        // Mock validation chain - successful validation
        when(validationChain.validate(any(BookingDTO.class), any(Account.class)))
            .thenReturn(ValidationResult.success());
        
        // Store facility in validation context with specific attributes to verify it's used
        Facility customFacility = Facility.builder()
                .facilityId(42L)
                .resourceTypeId(7L)
                .resourceName("Custom Test Facility")
                .build();
        BookingValidationContext.setFacility(customFacility);
        
        // Mock credit repository
        when(creditRepository.checkAndDeductCredits(anyLong(), anyDouble())).thenReturn(1);
        
        // Mock booking repository - capture the saved booking to verify facility
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking savedBooking = invocation.getArgument(0);
            return savedBooking;
        });
        
        // Mock booking mapper
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(bookingResponseDTO);
        
        try {
            // Call method under test
            bookingService.createBooking(bookingDTO);
            
            // Verify the facility from context was used to create the booking
            ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(bookingCaptor.capture());
            
            Booking capturedBooking = bookingCaptor.getValue();
            assertNotNull(capturedBooking.getFacility());
            assertEquals(42L, capturedBooking.getFacility().getFacilityId());
            assertEquals("Custom Test Facility", capturedBooking.getFacility().getResourceName());
            assertEquals(7L, capturedBooking.getFacility().getResourceTypeId());
        } finally {
            // Clean up context
            BookingValidationContext.clear();
        }
    }
    
    @Test
    @DisplayName("Get resource types should handle empty results")
    void getResourceTypesShouldHandleEmptyResults() {
        // Mock empty result
        when(facilityTypeRepository.findAllFacilityTypeOptions()).thenReturn(Collections.emptyList());
        
        List<FacilityNameOptionsResponse> result = bookingService.getResourceTypes();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(facilityTypeRepository).findAllFacilityTypeOptions();
    }
}