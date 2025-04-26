package com.group8.rbs.service.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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
                .bookings(Arrays.asList())
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
    void searchFacilitiesShouldReturnFilteredFacilities() {
        when(facilityRepository.searchFacilities(any(), any(), any(), any())).thenReturn(facilities);
        when(bookingFacilityMapper.toResponseDTO(any(Facility.class))).thenReturn(facilitySearchResults.get(0));
        
        List<FacilitySearchDTO> result = bookingService.searchFacilities(searchDTO);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(facilityRepository).searchFacilities(any(), any(), any(), any());
        verify(bookingFacilityMapper).toResponseDTO(any(Facility.class));
    }

    @Test
    void createBookingShouldReturnBookingResponse() {
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(facility));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(creditRepository.checkAndDeductCredits(anyLong(), anyDouble())).thenReturn(1);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(bookingResponseDTO);
        
        // Mock time slot availability
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(Arrays.asList());
        
        BookingResponseDTO result = bookingService.createBooking(bookingDTO);
        
        assertNotNull(result);
        assertEquals(1L, result.getBookingId());
        assertEquals("APPROVED", result.getStatus());
        verify(bookingRepository).save(any(Booking.class));
    }
    
    @Test
    void createBookingShouldThrowExceptionWhenFacilityNotFound() {
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> bookingService.createBooking(bookingDTO));
    }
    
    @Test
    void createBookingShouldThrowExceptionWhenAccountNotFound() {
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(facility));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> bookingService.createBooking(bookingDTO));
    }
    
    @Test
    void createBookingShouldThrowExceptionWhenInsufficientCredits() {
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(facility));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(creditRepository.checkAndDeductCredits(anyLong(), anyDouble())).thenReturn(0);
        when(creditRepository.findCreditBalanceByAccountId(anyLong())).thenReturn(30.0);
        
        // Mock time slot availability
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(Arrays.asList());
        
        assertThrows(RuntimeException.class, () -> bookingService.createBooking(bookingDTO));
    }
    
    @Test
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
    void deleteBookingShouldReturnTrueWhenSuccessful() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        
        boolean result = bookingService.deleteBooking(1L);
        
        assertTrue(result);
        verify(bookingRepository).delete(any(Booking.class));
    }
    
    @Test
    void deleteBookingShouldReturnFalseWhenBookingNotFound() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        boolean result = bookingService.deleteBooking(1L);
        
        assertFalse(result);
        verify(bookingRepository, never()).delete(any(Booking.class));
    }
    
    @Test
    void updateBookingStatusShouldReturnTrueWhenSuccessful() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        
        boolean result = bookingService.updateBookingStatus(1L, BookingStatus.CONFIRMED);
        
        assertTrue(result);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository).save(any(Booking.class));
    }
    
    @Test
    void updateBookingStatusShouldReturnFalseWhenBookingNotFound() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        boolean result = bookingService.updateBookingStatus(1L, BookingStatus.CONFIRMED);
        
        assertFalse(result);
        verify(bookingRepository, never()).save(any(Booking.class));
    }
    
    @Test
    void getDropdownOptionsShouldReturnAllOptions() {
        // Mock resource types
        Object[] resourceTypeArray = new Object[]{ 1L, "Meeting Room" };
        List<Object[]> resourceTypeResults = new ArrayList<>();
        resourceTypeResults.add(resourceTypeArray);
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
        
        verify(facilityTypeRepository).findAllFacilityTypeOptions();
        verify(facilityRepository).findAllLocations();
        verify(facilityRepository).findAllResourceNames();
    }
    
    @Test
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
}