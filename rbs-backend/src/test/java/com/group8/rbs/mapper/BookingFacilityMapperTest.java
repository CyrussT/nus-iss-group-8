package com.group8.rbs.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.enums.AccountType;
import com.group8.rbs.enums.BookingStatus;

@ExtendWith(MockitoExtension.class)
public class BookingFacilityMapperTest {

    @Mock
    private BookingMapper bookingMapper;
    
    @InjectMocks
    private BookingFacilityMapper bookingFacilityMapper;
    
    private Facility facility;
    private Account account;
    private List<Booking> bookings;
    private LocalDate today;
    private BookingResponseDTO mockBookingResponse;
    
    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        
        // Setup Account
        account = Account.builder()
                .accountId(1L)
                .email("test@example.com")
                .name("Test Student")
                .accountType(AccountType.STUDENT)
                .build();
        
        // Setup Facility (without bookings yet)
        facility = Facility.builder()
                .facilityId(1L)
                .resourceTypeId(1L)
                .resourceName("Test Facility")
                .location("Test Location")
                .capacity(10)
                .build();
        
        // Setup mock BookingResponseDTO
        mockBookingResponse = BookingResponseDTO.builder()
                .bookingId(1L)
                .studentId("1")
                .studentName("Test Student")
                .facilityName("Test Facility")
                .location("Test Location")
                .bookedDatetime(today.atTime(10, 0))
                .timeslot("10:00 - 11:00")
                .status("APPROVED")
                .build();
        
        // Create bookings with different statuses and dates
        bookings = new ArrayList<>();
        
        // Add APPROVED booking today
        Booking approvedBooking = Booking.builder()
                .bookingId(1L)
                .account(account)
                .facility(facility)
                .bookedDateTime(today.atTime(10, 0))
                .timeSlot("10:00 - 11:00")
                .status(BookingStatus.APPROVED)
                .build();
        bookings.add(approvedBooking);
        
        // Add PENDING booking today
        Booking pendingBooking = Booking.builder()
                .bookingId(2L)
                .account(account)
                .facility(facility)
                .bookedDateTime(today.atTime(12, 0))
                .timeSlot("12:00 - 13:00")
                .status(BookingStatus.PENDING)
                .build();
        bookings.add(pendingBooking);
        
        // Add CONFIRMED booking tomorrow
        Booking confirmedBooking = Booking.builder()
                .bookingId(3L)
                .account(account)
                .facility(facility)
                .bookedDateTime(today.plusDays(1).atTime(14, 0))
                .timeSlot("14:00 - 15:00")
                .status(BookingStatus.CONFIRMED)
                .build();
        bookings.add(confirmedBooking);
        
        // Add CANCELLED booking today
        Booking cancelledBooking = Booking.builder()
                .bookingId(4L)
                .account(account)
                .facility(facility)
                .bookedDateTime(today.atTime(16, 0))
                .timeSlot("16:00 - 17:00")
                .status(BookingStatus.CANCELLED)
                .build();
        bookings.add(cancelledBooking);
        
        // Add REJECTED booking today
        Booking rejectedBooking = Booking.builder()
                .bookingId(5L)
                .account(account)
                .facility(facility)
                .bookedDateTime(today.atTime(18, 0))
                .timeSlot("18:00 - 19:00")
                .status(BookingStatus.REJECTED)
                .build();
        bookings.add(rejectedBooking);
        
        // Set bookings to the facility
        facility.setBookings(bookings);
        
        // Using lenient() to prevent UnnecessaryStubbingException in tests that don't use this mock
        lenient().when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(mockBookingResponse);
    }
    
    @Test
    @DisplayName("Should map facility without bookings")
    void shouldMapFacilityWithoutBookings() {
        // Create a facility without bookings
        Facility facilityWithoutBookings = Facility.builder()
                .facilityId(1L)
                .resourceTypeId(1L)
                .resourceName("Test Facility")
                .location("Test Location")
                .capacity(10)
                .bookings(null) // No bookings
                .build();
        
        // Call the mapper
        FacilitySearchDTO result = bookingFacilityMapper.toResponseDTO(facilityWithoutBookings);
        
        // Verify basic facility fields are mapped correctly
        assertEquals(1L, result.getFacilityId());
        assertEquals(1L, result.getResourceTypeId());
        assertEquals("Test Facility", result.getResourceName());
        assertEquals("Test Location", result.getLocation());
        assertEquals(10, result.getCapacity());
        
        // Verify bookings list exists but is empty
        assertNotNull(result.getBookings());
        assertTrue(result.getBookings().isEmpty());
        
        // Verify BookingMapper was not called
        verify(bookingMapper, times(0)).toResponseDTO(any());
    }
    
    @Test
    @DisplayName("Should map facility with empty bookings list")
    void shouldMapFacilityWithEmptyBookingsList() {
        // Create a facility with empty bookings list
        Facility facilityWithEmptyBookings = Facility.builder()
                .facilityId(1L)
                .resourceTypeId(1L)
                .resourceName("Test Facility")
                .location("Test Location")
                .capacity(10)
                .bookings(Collections.emptyList()) // Empty list
                .build();
        
        // Call the mapper
        FacilitySearchDTO result = bookingFacilityMapper.toResponseDTO(facilityWithEmptyBookings);
        
        // Verify bookings list is empty
        assertTrue(result.getBookings().isEmpty());
        
        // Verify BookingMapper was not called
        verify(bookingMapper, times(0)).toResponseDTO(any());
    }
    
    @Test
    @DisplayName("Should map facility with all bookings (no date filter)")
    void shouldMapFacilityWithAllBookings() {
        // Call the mapper without date filter
        FacilitySearchDTO result = bookingFacilityMapper.toResponseDTO(facility);
        
        // Verify basic facility fields
        assertEquals(1L, result.getFacilityId());
        assertEquals("Test Facility", result.getResourceName());
        
        // Only APPROVED, CONFIRMED and PENDING bookings should be included (3 total)
        assertEquals(3, result.getBookings().size());
        
        // Verify BookingMapper was called 3 times
        verify(bookingMapper, times(3)).toResponseDTO(any());
    }
    
    @Test
    @DisplayName("Should filter bookings by date")
    void shouldFilterBookingsByDate() {
        // Call the mapper with date filter (today)
        FacilitySearchDTO result = bookingFacilityMapper.toResponseDTOWithDateFilter(facility, today);
        
        // Today we have 2 valid bookings (APPROVED and PENDING)
        assertEquals(2, result.getBookings().size());
        
        // Tomorrow we have 1 valid booking (CONFIRMED)
        FacilitySearchDTO resultTomorrow = bookingFacilityMapper.toResponseDTOWithDateFilter(facility, today.plusDays(1));
        assertEquals(1, resultTomorrow.getBookings().size());
        
        // Verify BookingMapper was called 3 times total
        verify(bookingMapper, times(3)).toResponseDTO(any());
    }
    
    @Test
    @DisplayName("Should filter out cancelled and rejected bookings")
    void shouldFilterOutCancelledAndRejectedBookings() {
        // Create a new list with only cancelled and rejected bookings
        List<Booking> onlyInvalidBookings = Arrays.asList(
            bookings.get(3), // CANCELLED
            bookings.get(4)  // REJECTED
        );
        
        Facility facilityWithInvalidBookings = Facility.builder()
                .facilityId(1L)
                .resourceTypeId(1L)
                .resourceName("Test Facility")
                .bookings(onlyInvalidBookings)
                .build();
        
        // Call the mapper
        FacilitySearchDTO result = bookingFacilityMapper.toResponseDTO(facilityWithInvalidBookings);
        
        // Verify no bookings are included
        assertTrue(result.getBookings().isEmpty());
        
        // Verify BookingMapper was not called
        verify(bookingMapper, times(0)).toResponseDTO(any());
    }
    
    @Test
    @DisplayName("Should filter by date and status correctly")
    void shouldFilterByDateAndStatusCorrectly() {
        // Create a list with mixed bookings on different dates
        Booking futureApproved = Booking.builder()
                .bookingId(6L)
                .account(account)
                .facility(facility)
                .bookedDateTime(today.plusDays(2).atTime(10, 0))
                .status(BookingStatus.APPROVED)
                .build();
                
        Booking futureRejected = Booking.builder()
                .bookingId(7L)
                .account(account)
                .facility(facility)
                .bookedDateTime(today.plusDays(2).atTime(12, 0))
                .status(BookingStatus.REJECTED)
                .build();
                
        List<Booking> mixedBookings = Arrays.asList(
            bookings.get(0), // Today APPROVED
            bookings.get(3), // Today CANCELLED
            futureApproved,  // Future APPROVED
            futureRejected   // Future REJECTED
        );
        
        Facility facilityWithMixedBookings = Facility.builder()
                .facilityId(1L)
                .resourceName("Test Facility")
                .bookings(mixedBookings)
                .build();
        
        // Call the mapper with future date filter
        FacilitySearchDTO result = bookingFacilityMapper.toResponseDTOWithDateFilter(
            facilityWithMixedBookings, today.plusDays(2));
        
        // Verify only the future approved booking is included (NOT the rejected one)
        assertEquals(1, result.getBookings().size());
        
        // Verify BookingMapper was called only once
        verify(bookingMapper, times(1)).toResponseDTO(any());
    }
    
    @Test
    @DisplayName("Should handle date filter that matches no bookings")
    void shouldHandleDateFilterThatMatchesNoBookings() {
        // Call the mapper with a date that has no bookings
        FacilitySearchDTO result = bookingFacilityMapper.toResponseDTOWithDateFilter(
            facility, today.plusDays(5));
        
        // Verify no bookings are included
        assertTrue(result.getBookings().isEmpty());
        
        // Verify BookingMapper was not called
        verify(bookingMapper, times(0)).toResponseDTO(any());
    }
    
    @Test
    @DisplayName("Should include date in resulting DTO when provided")
    void shouldIncludeDateInResultingDTOWhenProvided() {
        // Call the mapper with date filter
        FacilitySearchDTO result = bookingFacilityMapper.toResponseDTOWithDateFilter(facility, today);
        
        // Verify date is included in result
        assertEquals(today, result.getDate());
    }
}