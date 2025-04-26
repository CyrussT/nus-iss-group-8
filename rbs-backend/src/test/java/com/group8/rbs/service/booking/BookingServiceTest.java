package com.group8.rbs.service.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
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
    private BookingFacilityMapper bookingFacilityMapper;

    @InjectMocks
    private BookingService bookingService;

    private BookingDTO bookingDTO;
    private Booking booking;
    private BookingResponseDTO bookingResponseDTO;
    private Account account;
    private Facility facility;
    private FacilitySearchDTO searchDTO;
    private List<Facility> facilities;
    private List<FacilitySearchDTO> facilitySearchResults;

    @BeforeEach
    void setUp() {
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
                .bookedDateTime(LocalDateTime.now().plusDays(1))
                .timeSlot("14:00 - 15:00")
                .title("Test Booking")
                .description("Test Description")
                .status(BookingStatus.APPROVED)
                .build();
        
        // Setup BookingDTO
        bookingDTO = new BookingDTO();
        bookingDTO.setFacilityId(1L);
        bookingDTO.setAccountEmail("test@example.com");
        bookingDTO.setBookedDateTime(LocalDateTime.now().plusDays(1));
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
                .bookedDatetime(LocalDateTime.now().plusDays(1))
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
        Booking booking1 = Booking.builder()
                .bookingId(1L)
                .facility(facility)
                .account(account)
                .bookedDateTime(LocalDate.now().atTime(10, 0))
                .timeSlot("10:00 - 11:00")
                .status(BookingStatus.APPROVED)
                .build();
                
        Booking booking2 = Booking.builder()
                .bookingId(2L)
                .facility(facility)
                .account(account)
                .bookedDateTime(LocalDate.now().plusDays(1).atTime(10, 0))
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
    @DisplayName("Create booking should return booking response")
    void createBookingShouldReturnBookingResponse() {
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(facility));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(creditRepository.checkAndDeductCredits(anyLong(), anyDouble())).thenReturn(1);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(bookingResponseDTO);
        
        // Mock time slot availability - return empty list indicating time slot is available
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(Collections.emptyList());
        
        BookingResponseDTO result = bookingService.createBooking(bookingDTO);
        
        assertNotNull(result);
        assertEquals(1L, result.getBookingId());
        assertEquals("APPROVED", result.getStatus());
        assertEquals("Test Facility", result.getFacilityName());
        assertEquals("Test Location", result.getLocation());
        verify(bookingRepository).save(any(Booking.class));
        verify(creditRepository).checkAndDeductCredits(anyLong(), anyDouble());
    }
    
    @Test
    @DisplayName("Create booking should throw exception when facility not found")
    void createBookingShouldThrowExceptionWhenFacilityNotFound() {
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> bookingService.createBooking(bookingDTO));
        
        assertEquals("Facility not found", exception.getMessage());
        verify(accountRepository, never()).findByEmail(anyString());
        verify(bookingRepository, never()).save(any(Booking.class));
    }
    
    @Test
    @DisplayName("Create booking should throw exception when account not found")
    void createBookingShouldThrowExceptionWhenAccountNotFound() {
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(facility));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> bookingService.createBooking(bookingDTO));
        
        assertEquals("Account not found", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }
    
    @Test
    @DisplayName("Create booking should throw exception when time slot is not available")
    void createBookingShouldThrowExceptionWhenTimeSlotIsNotAvailable() {
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(facility));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        
        // Mock time slot availability - return a booking with the same time slot
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(Arrays.asList(booking));
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> bookingService.createBooking(bookingDTO));
        
        assertEquals("This time slot is already booked", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(creditRepository, never()).checkAndDeductCredits(anyLong(), anyDouble());
    }
    
    @Test
    @DisplayName("Create booking should throw exception when insufficient credits")
    void createBookingShouldThrowExceptionWhenInsufficientCredits() {
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(facility));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(creditRepository.checkAndDeductCredits(anyLong(), anyDouble())).thenReturn(0);
        when(creditRepository.findCreditBalanceByAccountId(anyLong())).thenReturn(30.0);
        
        // Mock time slot availability
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(Collections.emptyList());
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> bookingService.createBooking(bookingDTO));
        
        assertTrue(exception.getMessage().contains("Insufficient credits"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }
    
    @Test
    @DisplayName("Create booking with sports facility should set PENDING status")
    void createBookingWithSportsFacilityShouldSetPendingStatus() {
        // Change facility type to sports (ID 5)
        facility.setResourceTypeId(5L);
        
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(facility));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(creditRepository.checkAndDeductCredits(anyLong(), anyDouble())).thenReturn(1);
        
        // Create a booking with PENDING status for verification
        Booking pendingBooking = Booking.builder()
                .bookingId(1L)
                .account(account)
                .facility(facility)
                .bookedDateTime(LocalDateTime.now().plusDays(1))
                .timeSlot("14:00 - 15:00")
                .title("Test Booking")
                .description("Test Description")
                .status(BookingStatus.PENDING)
                .build();
        
        // Mock save to return our pending booking
        when(bookingRepository.save(any(Booking.class))).thenReturn(pendingBooking);
        
        // Create response DTO with PENDING status
        BookingResponseDTO pendingResponseDTO = BookingResponseDTO.builder()
                .bookingId(1L)
                .status("PENDING")
                .build();
        
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(pendingResponseDTO);
        
        // Mock time slot availability
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(Collections.emptyList());
        
        BookingResponseDTO result = bookingService.createBooking(bookingDTO);
        
        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        
        // Verify the booking was saved
        verify(bookingRepository).save(any(Booking.class));
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
    @DisplayName("Get upcoming approved or confirmed bookings should handle empty list")
    void getUpcomingApprovedOrConfirmedBookingsShouldHandleEmptyList() {
        when(bookingRepository.findUpcomingApprovedOrConfirmedBookings(
                anyLong(), anyList(), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        
        List<BookingResponseDTO> result = bookingService.getUpcomingApprovedOrConfirmedBookings(1L);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
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
    @DisplayName("Get pending future bookings should handle empty list")
    void getPendingFutureBookingsShouldHandleEmptyList() {
        when(bookingRepository.findByAccount_AccountIdAndStatusAndBookedDateTimeAfter(
                anyLong(), any(BookingStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        
        List<BookingResponseDTO> result = bookingService.getPendingFutureBookings(1L);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
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
    
    @Test
    @DisplayName("Get bookings by status should handle empty list")
    void getBookingsByStatusShouldHandleEmptyList() {
        when(bookingRepository.findByStatus(any(BookingStatus.class)))
                .thenReturn(Collections.emptyList());
        
        List<BookingResponseDTO> result = bookingService.getBookingsByStatus("APPROVED");
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("Is time slot available should return true when slot is available")
    void isTimeSlotAvailableShouldReturnTrueWhenSlotIsAvailable() {
        // Method uses a private method, so we'll test it through createBooking
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(facility));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(creditRepository.checkAndDeductCredits(anyLong(), anyDouble())).thenReturn(1);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(bookingResponseDTO);
        
        // Mock empty list to indicate time slot is available
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(Collections.emptyList());
        
        // This should succeed since time slot is available
        BookingResponseDTO result = bookingService.createBooking(bookingDTO);
        
        assertNotNull(result);
        // Verify findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn was called
        verify(bookingRepository).findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList());
    }
}