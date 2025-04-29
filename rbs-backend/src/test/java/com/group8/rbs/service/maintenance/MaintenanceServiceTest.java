package com.group8.rbs.service.maintenance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.group8.rbs.entities.Booking;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.MaintenanceSchedule;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.exception.MaintenanceOverlapException;
import com.group8.rbs.repository.BookingRepository;
import com.group8.rbs.repository.FacilityRepository;
import com.group8.rbs.repository.MaintenanceRepository;
import com.group8.rbs.service.credit.CreditService;
import com.group8.rbs.service.email.CustomEmailService;

import jakarta.mail.MessagingException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MaintenanceServiceTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private FacilityRepository facilityRepository;
    
    @Mock
    private CreditService creditService;
    
    @Mock
    private CustomEmailService emailService;
    
    // Create a spy instead of just injecting mocks
    @Spy
    @InjectMocks
    private MaintenanceService maintenanceService;
    
    private MaintenanceSchedule maintenanceSchedule;
    private List<Booking> affectedBookings;
    private Account testAccount;
    private Facility testFacility;
    private Booking testBooking;
    private LocalDate today;
    private LocalDate futureDate;
    
    @BeforeEach
    void setUp() {
        // Setup dates
        today = LocalDate.now();
        futureDate = today.plusDays(5); 
        
        // Setup MaintenanceSchedule with current dates for testing
        maintenanceSchedule = MaintenanceSchedule.builder()
                .maintenanceId(1L)
                .facilityId(1L)
                .startDate(today.toString())
                .endDate(futureDate.toString())
                .description("Test Maintenance")
                .createdBy(1L)
                .build();
        
        // Setup test account
        testAccount = Account.builder()
                .accountId(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
        
        // Setup facility
        testFacility = Facility.builder()
                .facilityId(1L)
                .resourceName("Test Facility")
                .location("Test Location")
                .build();
                
        // Setup affected bookings
        testBooking = Booking.builder()
                .bookingId(1L)
                .facility(testFacility)
                .account(testAccount)
                .bookedDateTime(today.atTime(10, 0)) // Today, 10 AM
                .timeSlot("10:00 - 11:00")
                .status(BookingStatus.APPROVED)
                .build();
                
        affectedBookings = Arrays.asList(testBooking);
    }
    
    @Test
    @DisplayName("Get all maintenance schedules should return list")
    void getAllMaintenanceSchedulesShouldReturnList() {
        when(maintenanceRepository.findAll()).thenReturn(Arrays.asList(maintenanceSchedule));
        
        List<MaintenanceSchedule> result = maintenanceService.getAllMaintenanceSchedules();
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getMaintenanceId());
        assertEquals("Test Maintenance", result.get(0).getDescription());
        verify(maintenanceRepository).findAll();
    }
    
    @Test
    @DisplayName("Get maintenance schedules for facility should return list")
    void getMaintenanceSchedulesForFacilityShouldReturnList() {
        when(maintenanceRepository.findByFacilityId(anyLong())).thenReturn(Arrays.asList(maintenanceSchedule));
        
        List<MaintenanceSchedule> result = maintenanceService.getMaintenanceSchedulesForFacility(1L);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getFacilityId());
        verify(maintenanceRepository).findByFacilityId(1L);
    }
    
    @Test
    @DisplayName("Get maintenance schedule by id should return schedule")
    void getMaintenanceScheduleByIdShouldReturnSchedule() {
        when(maintenanceRepository.findById(anyLong())).thenReturn(Optional.of(maintenanceSchedule));
        
        Optional<MaintenanceSchedule> result = maintenanceService.getMaintenanceScheduleById(1L);
        
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getMaintenanceId());
        assertEquals("Test Maintenance", result.get().getDescription());
        verify(maintenanceRepository).findById(1L);
    }
    
    @Test
    @DisplayName("Get maintenance schedule by id should handle not found")
    void getMaintenanceScheduleByIdShouldHandleNotFound() {
        when(maintenanceRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        Optional<MaintenanceSchedule> result = maintenanceService.getMaintenanceScheduleById(1L);
        
        assertFalse(result.isPresent());
        verify(maintenanceRepository).findById(1L);
    }
    
    @Test
    @DisplayName("Is facility under maintenance should return true when under maintenance")
    void isFacilityUnderMaintenanceShouldReturnTrueWhenUnderMaintenance() {
        when(maintenanceRepository.existsByFacilityIdAndCurrentDate(anyLong())).thenReturn(true);
        
        boolean result = maintenanceService.isFacilityUnderMaintenance(1L);
        
        assertTrue(result);
        verify(maintenanceRepository).existsByFacilityIdAndCurrentDate(1L);
    }
    
    @Test
    @DisplayName("Is facility under maintenance should return false when not under maintenance")
    void isFacilityUnderMaintenanceShouldReturnFalseWhenNotUnderMaintenance() {
        when(maintenanceRepository.existsByFacilityIdAndCurrentDate(anyLong())).thenReturn(false);
        
        boolean result = maintenanceService.isFacilityUnderMaintenance(1L);
        
        assertFalse(result);
        verify(maintenanceRepository).existsByFacilityIdAndCurrentDate(1L);
    }
    
    @Test
    @DisplayName("Is facility under maintenance on date should return true when under maintenance")
    void isFacilityUnderMaintenanceOnDateShouldReturnTrueWhenUnderMaintenance() {
        LocalDate testDate = today.plusDays(2); // Date within maintenance period
        
        // Mock findByFacilityId to return our test maintenance schedule
        when(maintenanceRepository.findByFacilityId(anyLong())).thenReturn(Arrays.asList(maintenanceSchedule));
        
        boolean result = maintenanceService.isFacilityUnderMaintenanceOnDate(1L, testDate);
        
        assertTrue(result);
        verify(maintenanceRepository).findByFacilityId(1L);
    }
    
    @Test
    @DisplayName("Is facility under maintenance on date should return false when not under maintenance")
    void isFacilityUnderMaintenanceOnDateShouldReturnFalseWhenNotUnderMaintenance() {
        LocalDate testDate = today.plusDays(10); // Date after maintenance period
        
        // Mock findByFacilityId to return our test maintenance schedule
        when(maintenanceRepository.findByFacilityId(anyLong())).thenReturn(Arrays.asList(maintenanceSchedule));
        
        boolean result = maintenanceService.isFacilityUnderMaintenanceOnDate(1L, testDate);
        
        assertFalse(result);
        verify(maintenanceRepository).findByFacilityId(1L);
    }
    
    @Test
    @DisplayName("Get current maintenance for facility should return schedule when under maintenance")
    void getCurrentMaintenanceForFacilityShouldReturnScheduleWhenUnderMaintenance() {
        // Mock findActiveMaintenance method to return our test schedule
        when(maintenanceRepository.findActiveMaintenance()).thenReturn(Arrays.asList(maintenanceSchedule));
        
        Optional<MaintenanceSchedule> result = maintenanceService.getCurrentMaintenanceForFacility(1L);
        
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getMaintenanceId());
        assertEquals(1L, result.get().getFacilityId());
        verify(maintenanceRepository).findActiveMaintenance();
    }
    
    @Test
    @DisplayName("Get current maintenance for facility should return empty when not under maintenance")
    void getCurrentMaintenanceForFacilityShouldReturnEmptyWhenNotUnderMaintenance() {
        // Change facility ID in maintenance schedule
        maintenanceSchedule.setFacilityId(2L);
        
        // Mock findActiveMaintenance method to return our modified test schedule
        when(maintenanceRepository.findActiveMaintenance()).thenReturn(Arrays.asList(maintenanceSchedule));
        
        Optional<MaintenanceSchedule> result = maintenanceService.getCurrentMaintenanceForFacility(1L);
        
        assertFalse(result.isPresent());
        verify(maintenanceRepository).findActiveMaintenance();
    }
    
    @Test
    @DisplayName("Get maintenance for facility on date should return schedule when under maintenance")
    void getMaintenanceForFacilityOnDateShouldReturnScheduleWhenUnderMaintenance() {
        LocalDate testDate = today.plusDays(2); // Date within maintenance period
        
        // Mock findByFacilityId to return our test maintenance schedule
        when(maintenanceRepository.findByFacilityId(anyLong())).thenReturn(Arrays.asList(maintenanceSchedule));
        
        Optional<MaintenanceSchedule> result = maintenanceService.getMaintenanceForFacilityOnDate(1L, testDate);
        
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getMaintenanceId());
        assertEquals(1L, result.get().getFacilityId());
        verify(maintenanceRepository).findByFacilityId(1L);
    }
    
    @Test
    @DisplayName("Get maintenance for facility on date should return empty when not under maintenance")
    void getMaintenanceForFacilityOnDateShouldReturnEmptyWhenNotUnderMaintenance() {
        LocalDate testDate = today.plusDays(10); // Date after maintenance period
        
        // Mock findByFacilityId to return our test maintenance schedule
        when(maintenanceRepository.findByFacilityId(anyLong())).thenReturn(Arrays.asList(maintenanceSchedule));
        
        Optional<MaintenanceSchedule> result = maintenanceService.getMaintenanceForFacilityOnDate(1L, testDate);
        
        assertFalse(result.isPresent());
        verify(maintenanceRepository).findByFacilityId(1L);
    }
    
    @Test
    @DisplayName("Schedule maintenance for facility should return saved schedule")
    void scheduleMaintenanceForFacilityShouldReturnSavedSchedule() {
        // Mock overlap check
        doReturn(false).when(maintenanceService).hasOverlappingMaintenance(
                anyLong(), anyString(), anyString(), any());
        
        when(maintenanceRepository.save(any(MaintenanceSchedule.class))).thenReturn(maintenanceSchedule);
        
        MaintenanceSchedule result = maintenanceService.scheduleMaintenanceForFacility(maintenanceSchedule);
        
        assertNotNull(result);
        assertEquals(1L, result.getMaintenanceId());
        assertEquals("Test Maintenance", result.getDescription());
        verify(maintenanceRepository).save(any(MaintenanceSchedule.class));
    }
    
    @Test
    @DisplayName("Schedule maintenance for facility should throw exception when overlap exists")
    void scheduleMaintenanceForFacilityShouldThrowExceptionWhenOverlapExists() {
        // Mock overlap check to return true
        doReturn(true).when(maintenanceService).hasOverlappingMaintenance(
                anyLong(), anyString(), anyString(), any());
        
        // Verify exception is thrown
        MaintenanceOverlapException exception = assertThrows(MaintenanceOverlapException.class, 
                () -> maintenanceService.scheduleMaintenanceForFacility(maintenanceSchedule));
        
        assertTrue(exception.getMessage().contains("already has scheduled maintenance"));
        verify(maintenanceRepository, never()).save(any(MaintenanceSchedule.class));
    }
    
    @Test
    @DisplayName("Has overlapping maintenance should return true when overlap exists")
    void hasOverlappingMaintenanceShouldReturnTrueWhenOverlapExists() {
        // Create overlapping schedule in repository
        MaintenanceSchedule overlappingSchedule = MaintenanceSchedule.builder()
                .maintenanceId(2L)
                .facilityId(1L)
                .startDate(today.plusDays(2).toString()) // Overlaps with our test schedule
                .endDate(today.plusDays(7).toString())
                .build();
        
        // Mock repository
        when(maintenanceRepository.findByFacilityId(anyLong()))
                .thenReturn(Arrays.asList(overlappingSchedule));
        
        // Test for overlap
        boolean result = maintenanceService.hasOverlappingMaintenance(
                1L, today.toString(), futureDate.toString(), null);
        
        assertTrue(result);
        verify(maintenanceRepository).findByFacilityId(1L);
    }
    
    @Test
    @DisplayName("Has overlapping maintenance should return false when no overlap")
    void hasOverlappingMaintenanceShouldReturnFalseWhenNoOverlap() {
        // Create non-overlapping schedule in repository
        MaintenanceSchedule nonOverlappingSchedule = MaintenanceSchedule.builder()
                .maintenanceId(2L)
                .facilityId(1L)
                .startDate(today.plusDays(10).toString()) // After our test schedule
                .endDate(today.plusDays(15).toString())
                .build();
        
        // Mock repository
        when(maintenanceRepository.findByFacilityId(anyLong()))
                .thenReturn(Arrays.asList(nonOverlappingSchedule));
        
        // Test for overlap
        boolean result = maintenanceService.hasOverlappingMaintenance(
                1L, today.toString(), futureDate.toString(), null);
        
        assertFalse(result);
        verify(maintenanceRepository).findByFacilityId(1L);
    }
    
    @Test
    @DisplayName("Update maintenance end date should return updated schedule")
    void updateMaintenanceEndDateShouldReturnUpdatedSchedule() {
        String newEndDate = today.plusDays(3).toString(); // Earlier than original end date
        
        when(maintenanceRepository.findById(anyLong())).thenReturn(Optional.of(maintenanceSchedule));
        
        // Mock save to return the updated schedule
        when(maintenanceRepository.save(any(MaintenanceSchedule.class))).thenAnswer(invocation -> {
            MaintenanceSchedule savedSchedule = invocation.getArgument(0);
            assertEquals(newEndDate, savedSchedule.getEndDate());
            return savedSchedule;
        });
        
        MaintenanceSchedule result = maintenanceService.updateMaintenanceEndDate(1L, newEndDate);
        
        assertNotNull(result);
        assertEquals(newEndDate, result.getEndDate());
        verify(maintenanceRepository).findById(1L);
        verify(maintenanceRepository).save(any(MaintenanceSchedule.class));
    }
    
    @Test
    @DisplayName("Update maintenance end date should throw exception when schedule not found")
    void updateMaintenanceEndDateShouldThrowExceptionWhenScheduleNotFound() {
        when(maintenanceRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> maintenanceService.updateMaintenanceEndDate(1L, "2023-12-01"));
        
        assertEquals("Maintenance schedule not found", exception.getMessage());
        verify(maintenanceRepository).findById(1L);
        verify(maintenanceRepository, never()).save(any(MaintenanceSchedule.class));
    }
    
    @Test
    @DisplayName("Find bookings affected by maintenance should return affected bookings")
    void findBookingsAffectedByMaintenanceShouldReturnAffectedBookings() {
        // Use LocalDateTime.now() as cutoff time
        LocalDateTime cutoffTime = LocalDateTime.now();
        
        // Mock booking repository to return affected bookings
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(affectedBookings);
        
        List<Booking> result = maintenanceService.findBookingsAffectedByMaintenance(
                1L, today.toString(), futureDate.toString(), cutoffTime);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getBookingId());
        verify(bookingRepository).findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), anyList());
    }
    
    @Test
    @DisplayName("Cancel bookings for maintenance should cancel affected bookings and refund credits")
    void cancelBookingsForMaintenanceShouldCancelAffectedBookingsAndRefundCredits() throws MessagingException {
        // Mock facility repository
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(testFacility));
        
        // Mock successful email sending
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);
        
        // Mock booking repository save
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        
        int result = maintenanceService.cancelBookingsForMaintenance(
                affectedBookings, maintenanceSchedule, emailService);
        
        assertEquals(1, result);
        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
        
        // Verify credit service was called to add credits back
        verify(creditService).addCredits(eq(1L), any(Integer.class));
        
        // Verify email was sent
        verify(emailService).sendEmail(eq("test@example.com"), anyString(), anyString());
        
        // Verify booking was saved
        verify(bookingRepository).save(testBooking);
    }
    
    @Test
    @DisplayName("Cancel bookings for maintenance should handle empty bookings list")
    void cancelBookingsForMaintenanceShouldHandleEmptyBookingsList() throws MessagingException {
        int result = maintenanceService.cancelBookingsForMaintenance(
                Collections.emptyList(), maintenanceSchedule, emailService);
        
        assertEquals(0, result);
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("Cancel bookings for maintenance should handle email failure")
    void cancelBookingsForMaintenanceShouldHandleEmailFailure() throws MessagingException {
        // Mock facility repository
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(testFacility));
        
        // Mock failed email sending
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(false);
        
        // Mock booking repository save
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        
        int result = maintenanceService.cancelBookingsForMaintenance(
                affectedBookings, maintenanceSchedule, emailService);
        
        // Should still return 0 since email failed
        assertEquals(0, result);
        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
        
        // Verify credit service was still called to add credits back
        verify(creditService).addCredits(eq(1L), any(Integer.class));
        
        // Verify booking was saved
        verify(bookingRepository).save(testBooking);
    }
    
    @Test
    @DisplayName("Check maintenance status batch should return status map")
    void checkMaintenanceStatusBatchShouldReturnStatusMap() {
        List<Long> facilityIds = Arrays.asList(1L, 2L, 3L);
        List<Long> underMaintenanceIds = Arrays.asList(1L);
        
        when(maintenanceRepository.findFacilitiesUnderMaintenanceOnDate(anyList(), anyString()))
                .thenReturn(underMaintenanceIds);
        
        Map<String, Boolean> result = maintenanceService.checkMaintenanceStatusBatch(facilityIds, today);
        
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.get("1"));
        assertFalse(result.get("2"));
        assertFalse(result.get("3"));
        verify(maintenanceRepository).findFacilitiesUnderMaintenanceOnDate(anyList(), anyString());
    }
    
    @Test
    @DisplayName("Check maintenance status batch with mixed ID types should handle conversion")
    void checkMaintenanceStatusBatchWithMixedIDTypesShouldHandleConversion() {
        // Create list with mixed ID types
        List<Object> mixedIds = Arrays.asList(1L, 2, "3");
        List<Long> underMaintenanceIds = Arrays.asList(1L, 3L);
        
        when(maintenanceRepository.findFacilitiesUnderMaintenanceOnDate(anyList(), anyString()))
                .thenReturn(underMaintenanceIds);
        
        Map<String, Boolean> result = maintenanceService.checkMaintenanceStatusBatch(mixedIds, today);
        
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.get("1"));
        assertFalse(result.get("2"));
        assertTrue(result.get("3"));
        verify(maintenanceRepository).findFacilitiesUnderMaintenanceOnDate(anyList(), anyString());
    }
    
    @Test
    @DisplayName("Check maintenance status batch should handle empty facility IDs")
    void checkMaintenanceStatusBatchShouldHandleEmptyFacilityIDs() {
        Map<String, Boolean> result = maintenanceService.checkMaintenanceStatusBatch(
                Collections.emptyList(), today);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(maintenanceRepository, never()).findFacilitiesUnderMaintenanceOnDate(anyList(), anyString());
    }
    
    @Test
    @DisplayName("Check maintenance status batch with null date should use current date")
    void checkMaintenanceStatusBatchWithNullDateShouldUseCurrentDate() {
        List<Long> facilityIds = Arrays.asList(1L, 2L);
        List<Long> underMaintenanceIds = Arrays.asList(1L);
        
        when(maintenanceRepository.findFacilitiesUnderMaintenanceOnDate(anyList(), anyString()))
                .thenReturn(underMaintenanceIds);
        
        Map<String, Boolean> result = maintenanceService.checkMaintenanceStatusBatch(facilityIds, null);
        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get("1"));
        assertFalse(result.get("2"));
        verify(maintenanceRepository).findFacilitiesUnderMaintenanceOnDate(anyList(), anyString());
    }
    
    @Test
    @DisplayName("Calculate booking cost should return correct cost for time slot")
    void calculateBookingCostShouldReturnCorrectCostForTimeSlot() {
        // Use reflection to access private method
        try {
            java.lang.reflect.Method method = MaintenanceService.class.getDeclaredMethod(
                    "calculateBookingCost", String.class);
            method.setAccessible(true);
            
            // Test with 1-hour time slot (should be 1.0 credit)
            Integer result1 = (Integer) method.invoke(maintenanceService, "10:00 - 11:00");
            assertEquals(60, result1);
            
            // Test with 2-hour time slot (should be 2.0 credits)
            Integer result2 = (Integer) method.invoke(maintenanceService, "10:00 - 12:00");
            assertEquals(120, result2);
            
            // Test with 30-minute time slot (should be 0.5 credits)
            Integer result3 = (Integer) method.invoke(maintenanceService, "10:00 - 10:30");
            assertEquals(30, result3);
            
        } catch (Exception e) {
            fail("Failed to test calculateBookingCost method: " + e.getMessage());
        }
    }
}