package com.group8.rbs.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.enums.AccountType;
import com.group8.rbs.enums.BookingStatus;

@DataJpaTest
@ActiveProfiles("test")
public class BookingRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    private Account student1;
    private Account student2;
    private Facility facility1;
    private Facility facility2;
    private Booking approvedBooking;
    private Booking pendingBooking;
    private Booking pastBooking;
    private Booking futureBooking;
    
    @BeforeEach
    void setUp() {
        // Create test accounts
        student1 = Account.builder()
            .name("Student One")
            .email("student1@example.com")
            .accountType(AccountType.STUDENT)
            .status("Active")
            .password("password")
            .salt("salt")
            .build();
        
        student2 = Account.builder()
            .name("Student Two")
            .email("student2@example.com")
            .accountType(AccountType.STUDENT)
            .status("Active")
            .password("password")
            .salt("salt")
            .build();
        
        // Save accounts
        entityManager.persist(student1);
        entityManager.persist(student2);
        
        // Create test facilities
        facility1 = Facility.builder()
            .resourceTypeId(1L)
            .resourceName("Meeting Room 1")
            .location("Building A")
            .capacity(10)
            .build();
        
        facility2 = Facility.builder()
            .resourceTypeId(2L)
            .resourceName("Study Room 1")
            .location("Building B")
            .capacity(4)
            .build();
        
        // Save facilities
        entityManager.persist(facility1);
        entityManager.persist(facility2);
        
        // Create test bookings
        LocalDateTime now = LocalDateTime.now();
        
        // Approved future booking for student1
        approvedBooking = Booking.builder()
            .account(student1)
            .facility(facility1)
            .bookedDateTime(now.plusDays(1))
            .timeSlot("10:00 - 11:00")
            .title("Team Meeting")
            .description("Weekly team meeting")
            .status(BookingStatus.APPROVED)
            .build();
        
        // Pending future booking for student1
        pendingBooking = Booking.builder()
            .account(student1)
            .facility(facility2)
            .bookedDateTime(now.plusDays(2))
            .timeSlot("14:00 - 15:00")
            .title("Group Study")
            .description("Study for exams")
            .status(BookingStatus.PENDING)
            .build();
        
        // Past booking for student1
        pastBooking = Booking.builder()
            .account(student1)
            .facility(facility1)
            .bookedDateTime(now.minusDays(5))
            .timeSlot("09:00 - 10:00")
            .title("Past Meeting")
            .description("Previous meeting")
            .status(BookingStatus.CONFIRMED)
            .build();
        
        // Future booking for student2
        futureBooking = Booking.builder()
            .account(student2)
            .facility(facility1)
            .bookedDateTime(now.plusDays(3))
            .timeSlot("11:00 - 12:00")
            .title("Other Student Booking")
            .description("Another student's booking")
            .status(BookingStatus.APPROVED)
            .build();
        
        // Save bookings
        entityManager.persist(approvedBooking);
        entityManager.persist(pendingBooking);
        entityManager.persist(pastBooking);
        entityManager.persist(futureBooking);
        
        entityManager.flush();
    }
    
    @Test
    @DisplayName("Find bookings by account ID")
    void testFindByAccount_AccountId() {
        List<Booking> bookings = bookingRepository.findByAccount_AccountId(student1.getAccountId());
        
        assertEquals(3, bookings.size());
        assertTrue(bookings.contains(approvedBooking));
        assertTrue(bookings.contains(pendingBooking));
        assertTrue(bookings.contains(pastBooking));
        assertFalse(bookings.contains(futureBooking));
    }
    
    @Test
    @DisplayName("Find bookings by account ID and status")
    void testFindByAccount_AccountIdAndStatus() {
        List<Booking> bookings = bookingRepository.findByAccount_AccountIdAndStatus(
            student1.getAccountId(), BookingStatus.APPROVED);
        
        assertEquals(1, bookings.size());
        assertEquals(approvedBooking, bookings.get(0));
    }
    
    @Test
    @DisplayName("Find past bookings by account ID")
    void testFindByAccount_AccountIdAndBookedDateTimeBefore() {
        List<Booking> bookings = bookingRepository.findByAccount_AccountIdAndBookedDateTimeBefore(
            student1.getAccountId(), LocalDateTime.now());
        
        assertEquals(1, bookings.size());
        assertEquals(pastBooking, bookings.get(0));
    }
    
    @Test
    @DisplayName("Find past bookings by account ID and status")
    void testFindByAccount_AccountIdAndStatusAndBookedDateTimeBefore() {
        List<Booking> bookings = bookingRepository.findByAccount_AccountIdAndStatusAndBookedDateTimeBefore(
            student1.getAccountId(), BookingStatus.CONFIRMED, LocalDateTime.now());
        
        assertEquals(1, bookings.size());
        assertEquals(pastBooking, bookings.get(0));
    }
    
    @Test
    @DisplayName("Find future bookings by account ID and status")
    void testFindByAccount_AccountIdAndStatusAndBookedDateTimeAfter() {
        List<Booking> bookings = bookingRepository.findByAccount_AccountIdAndStatusAndBookedDateTimeAfter(
            student1.getAccountId(), BookingStatus.PENDING, LocalDateTime.now());
        
        assertEquals(1, bookings.size());
        assertEquals(pendingBooking, bookings.get(0));
    }
    
    @Test
    @DisplayName("Find bookings for facility within date range and status")
    void testFindByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusDays(10);
        LocalDateTime endTime = now.plusDays(10);
        
        List<BookingStatus> statuses = Arrays.asList(BookingStatus.APPROVED, BookingStatus.CONFIRMED, BookingStatus.PENDING);
        
        List<Booking> bookings = bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
            facility1.getFacilityId(), startTime, endTime, statuses);
        
        assertEquals(3, bookings.size());
        assertTrue(bookings.contains(approvedBooking));
        assertTrue(bookings.contains(pastBooking));
        assertTrue(bookings.contains(futureBooking));
    }
    
    @Test
    @DisplayName("Find upcoming approved or confirmed bookings")
    void testFindUpcomingApprovedOrConfirmedBookings() {
        List<BookingStatus> statuses = Arrays.asList(BookingStatus.APPROVED, BookingStatus.CONFIRMED);
        
        List<Booking> bookings = bookingRepository.findUpcomingApprovedOrConfirmedBookings(
            student1.getAccountId(), statuses, LocalDateTime.now());
        
        assertEquals(1, bookings.size());
        assertEquals(approvedBooking, bookings.get(0));
    }
    
    @Test
    @DisplayName("Find bookings by facility ID")
    void testFindByFacility_FacilityId() {
        List<Booking> bookings = bookingRepository.findByFacility_FacilityId(facility1.getFacilityId());
        
        assertEquals(3, bookings.size());
        assertTrue(bookings.contains(approvedBooking));
        assertTrue(bookings.contains(pastBooking));
        assertTrue(bookings.contains(futureBooking));
    }
    
    @Test
    @DisplayName("Find bookings by status")
    void testFindByStatus() {
        List<Booking> bookings = bookingRepository.findByStatus(BookingStatus.APPROVED);
        
        assertEquals(2, bookings.size());
        assertTrue(bookings.contains(approvedBooking));
        assertTrue(bookings.contains(futureBooking));
    }
}