package com.group8.rbs.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.enums.AccountType;
import com.group8.rbs.enums.BookingStatus;

public class BookingMapperTest {

    private BookingMapper bookingMapper;
    private Booking booking;
    private Account account;
    private Facility facility;
    private LocalDateTime bookedDateTime;

    @BeforeEach
    void setUp() {
        bookingMapper = new BookingMapper();
        bookedDateTime = LocalDateTime.now().plusDays(1);
        
        // Setup Account
        account = Account.builder()
                .accountId(1L)
                .email("test@example.com")
                .name("Test Student")
                .studentId("S12345")
                .accountType(AccountType.STUDENT)
                .build();
        
        // Setup Facility
        facility = Facility.builder()
                .facilityId(1L)
                .resourceTypeId(1L)
                .resourceName("Test Facility")
                .location("Test Location")
                .capacity(10)
                .build();
        
        // Setup Booking
        booking = Booking.builder()
                .bookingId(1L)
                .account(account)
                .facility(facility)
                .bookedDateTime(bookedDateTime)
                .timeSlot("14:00 - 15:00")
                .title("Test Booking")
                .description("Test Description")
                .status(BookingStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("Should map Booking to BookingResponseDTO with all fields")
    void shouldMapBookingToBookingResponseDTOWithAllFields() {
        // Call the mapper
        BookingResponseDTO response = bookingMapper.toResponseDTO(booking);
        
        // Verify all fields are mapped correctly
        assertEquals(1L, response.getBookingId());
        assertEquals("S12345", response.getStudentId());
        assertEquals("Test Student", response.getStudentName());
        assertEquals("Test Facility", response.getFacilityName());
        assertEquals("Test Location", response.getLocation());
        assertEquals(bookedDateTime, response.getBookedDatetime());
        assertEquals("14:00 - 15:00", response.getTimeslot());
        assertEquals("APPROVED", response.getStatus());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test Booking", response.getTitle());
        assertEquals("Test Description", response.getDescription());
    }
    
    @Test
    @DisplayName("Should handle null account fields")
    void shouldHandleNullAccountFields() {
        // Create booking with account having null fields
        Account incompleteAccount = Account.builder()
                .accountId(1L)
                .email(null)  // Null email
                .name(null)   // Null name
                .studentId(null)  // Null student ID
                .build();
        
        Booking bookingWithIncompleteAccount = Booking.builder()
                .bookingId(1L)
                .account(incompleteAccount)
                .facility(facility)
                .bookedDateTime(bookedDateTime)
                .timeSlot("14:00 - 15:00")
                .status(BookingStatus.APPROVED) // Using a non-null status
                .build();
        
        // Call the mapper
        BookingResponseDTO response = bookingMapper.toResponseDTO(bookingWithIncompleteAccount);
        
        // Verify fields are still mapped, with nulls preserved
        assertEquals(1L, response.getBookingId());
        assertNull(response.getStudentId());
        assertNull(response.getStudentName());
        assertNull(response.getEmail());
        assertEquals("APPROVED", response.getStatus()); // Status should be mapped
    }
    
    @Test
    @DisplayName("Should handle null facility fields")
    void shouldHandleNullFacilityFields() {
        // Create booking with facility having null fields
        Facility incompleteFacility = Facility.builder()
                .facilityId(1L)
                .resourceName(null)  // Null resource name
                .location(null)      // Null location
                .build();
        
        Booking bookingWithIncompleteFacility = Booking.builder()
                .bookingId(1L)
                .account(account)
                .facility(incompleteFacility)
                .bookedDateTime(bookedDateTime)
                .timeSlot("14:00 - 15:00")
                .status(BookingStatus.APPROVED) // Using a non-null status
                .build();
        
        // Call the mapper
        BookingResponseDTO response = bookingMapper.toResponseDTO(bookingWithIncompleteFacility);
        
        // Verify fields are still mapped, with nulls preserved
        assertEquals(1L, response.getBookingId());
        assertNull(response.getFacilityName());
        assertNull(response.getLocation());
        assertEquals("APPROVED", response.getStatus()); // Status should be mapped
    }
    
    @Test
    @DisplayName("Should handle null booking fields except status")
    void shouldHandleNullBookingFields() {
        // Create booking with null fields but VALID status
        Booking incompleteBooking = Booking.builder()
                .bookingId(1L)
                .account(account)
                .facility(facility)
                .bookedDateTime(null)      // Null date time
                .timeSlot(null)            // Null time slot
                .title(null)               // Null title
                .description(null)         // Null description
                .status(BookingStatus.PENDING)  // Non-null status
                .build();
        
        // Call the mapper
        BookingResponseDTO response = bookingMapper.toResponseDTO(incompleteBooking);
        
        // Verify fields are still mapped, with nulls preserved
        assertEquals(1L, response.getBookingId());
        assertNull(response.getBookedDatetime());
        assertNull(response.getTimeslot());
        assertNull(response.getTitle());
        assertNull(response.getDescription());
        assertEquals("PENDING", response.getStatus());  // Status should be mapped
    }
    
    @Test
    @DisplayName("Should handle different booking statuses")
    void shouldHandleDifferentBookingStatuses() {
        // Test for each booking status
        for (BookingStatus status : BookingStatus.values()) {
            booking.setStatus(status);
            BookingResponseDTO response = bookingMapper.toResponseDTO(booking);
            assertEquals(status.toString(), response.getStatus());
        }
    }
}