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
import org.mockito.ArgumentCaptor;
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
import com.group8.rbs.validation.ValidationResult;

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
    @DisplayName("Has overlapping maintenance should exclude current maintenance")
    void hasOverlappingMaintenanceShouldExcludeCurrentMaintenance() {
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
        
        // Test for overlap, excluding the overlapping schedule
        boolean result = maintenanceService.hasOverlappingMaintenance(
                1L, today.toString(), futureDate.toString(), 2L);
        
        assertFalse(result); // Should be false because we excluded the overlapping one
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
    @DisplayName("Find bookings affected by maintenance should handle different start times")
    void findBookingsAffectedByMaintenanceShouldHandleDifferentStartTimes() {
        // Today start date - should use cutoff time
        LocalDateTime cutoffTime = today.atTime(14, 0); // 2 PM today
        
        // Future start date - should use start of day
        LocalDate futureStartDate = today.plusDays(3);
        
        // Mock booking repository
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(affectedBookings);
        
        // Test with today
        maintenanceService.findBookingsAffectedByMaintenance(
                1L, today.toString(), futureDate.toString(), cutoffTime);
        
        // Verify start time is cutoff time for today
        ArgumentCaptor<LocalDateTime> startTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(bookingRepository).findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                eq(1L), startTimeCaptor.capture(), any(LocalDateTime.class), anyList());
        
        LocalDateTime capturedStartTime = startTimeCaptor.getValue();
        assertEquals(cutoffTime.getHour(), capturedStartTime.getHour());
        assertEquals(cutoffTime.getMinute(), capturedStartTime.getMinute());
        
        // Reset mock and test with future date
        reset(bookingRepository);
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(affectedBookings);
        
        maintenanceService.findBookingsAffectedByMaintenance(
                1L, futureStartDate.toString(), futureDate.toString(), cutoffTime);
        
        // Verify start time is start of day for future date
        verify(bookingRepository).findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                eq(1L), startTimeCaptor.capture(), any(LocalDateTime.class), anyList());
        
        capturedStartTime = startTimeCaptor.getValue();
        assertEquals(0, capturedStartTime.getHour());
        assertEquals(0, capturedStartTime.getMinute());
        assertEquals(futureStartDate, capturedStartTime.toLocalDate());
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
        
        // Mock credit service add credits
        when(creditService.addCredits(anyLong(), any(Integer.class))).thenReturn(true);
        
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
    @DisplayName("Calculate booking cost should return correct cost for time slot")
    void calculateBookingCostShouldReturnCorrectCostForTimeSlot() {
        // Use reflection to access private method
        try {
            java.lang.reflect.Method method = MaintenanceService.class.getDeclaredMethod(
                    "calculateBookingCost", String.class);
            method.setAccessible(true);
            
            // Test with 1-hour time slot (should be 60 minutes)
            Integer result1 = (Integer) method.invoke(maintenanceService, "10:00 - 11:00");
            assertEquals(60, result1);
            
            // Test with 2-hour time slot (should be 120 minutes)
            Integer result2 = (Integer) method.invoke(maintenanceService, "10:00 - 12:00");
            assertEquals(120, result2);
            
            // Test with 30-minute time slot (should be 30 minutes)
            Integer result3 = (Integer) method.invoke(maintenanceService, "10:00 - 10:30");
            assertEquals(30, result3);
            
            // Test with null time slot (should return 0)
            Integer result4 = (Integer) method.invoke(maintenanceService, (Object)null);
            assertEquals(0, result4);
            
            // Test with invalid format (should return 0)
            Integer result5 = (Integer) method.invoke(maintenanceService, "invalid format");
            assertEquals(0, result5);
            
        } catch (Exception e) {
            fail("Failed to test calculateBookingCost method: " + e.getMessage());
        }
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
    @DisplayName("Check maintenance status batch should handle null date")
    void checkMaintenanceStatusBatchShouldHandleNullDate() {
        List<Long> facilityIds = Arrays.asList(1L, 2L);
        List<Long> underMaintenanceIds = Arrays.asList(1L);
        
        when(maintenanceRepository.findFacilitiesUnderMaintenanceOnDate(anyList(), anyString()))
                .thenReturn(underMaintenanceIds);
        
        Map<String, Boolean> result = maintenanceService.checkMaintenanceStatusBatch(facilityIds, null);
        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify the current date is used when null date is provided
        ArgumentCaptor<String> dateCaptor = ArgumentCaptor.forClass(String.class);
        verify(maintenanceRepository).findFacilitiesUnderMaintenanceOnDate(anyList(), dateCaptor.capture());
        
        String capturedDate = dateCaptor.getValue();
        assertEquals(today.toString(), capturedDate);
    }
    
    @Test
    @DisplayName("Cancel bookings handles email sending failure")
    void cancelBookingsHandlesEmailSendingFailure() throws MessagingException {
        // Mock facility repository
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(testFacility));
        
        // Mock failed email sending
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(false);
        
        // Mock booking repository save
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        
        // Mock credit service
        when(creditService.addCredits(anyLong(), any(Integer.class))).thenReturn(true);
        
        // Call method
        int result = maintenanceService.cancelBookingsForMaintenance(
                affectedBookings, maintenanceSchedule, emailService);
        
        // Should return 0 since email failed
        assertEquals(0, result);
        
        // Verify booking was still cancelled
        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
        verify(bookingRepository).save(testBooking);
        
        // Verify credits were still refunded
        verify(creditService).addCredits(eq(1L), any(Integer.class));
    }
    
    @Test
    @DisplayName("Cancel bookings handles email exception")
    void cancelBookingsHandlesEmailException() throws MessagingException {
        // Mock facility repository
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(testFacility));
        
        // Mock email exception
        when(emailService.sendEmail(anyString(), anyString(), anyString()))
            .thenThrow(new MessagingException("Test email exception"));
        
        // Mock booking repository save
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        
        // Mock credit service
        when(creditService.addCredits(anyLong(), any(Integer.class))).thenReturn(true);
        
        // Call method - should handle exception
        int result = maintenanceService.cancelBookingsForMaintenance(
                affectedBookings, maintenanceSchedule, emailService);
        
        // Should return 0 due to exception
        assertEquals(0, result);
        
        // Verify booking was still cancelled and saved
        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
        verify(bookingRepository).save(testBooking);
        
        // Verify credits were still refunded
        verify(creditService).addCredits(eq(1L), any(Integer.class));
    }

    @Test
    @DisplayName("Calculate booking cost should handle special time slot format variations")
    void calculateBookingCostShouldHandleSpecialTimeSlotFormatVariations() {
        try {
            java.lang.reflect.Method method = MaintenanceService.class.getDeclaredMethod(
                    "calculateBookingCost", String.class);
            method.setAccessible(true);
            
            // Test with different time slot formatting (no spaces)
            Integer result1 = (Integer) method.invoke(maintenanceService, "10:00-11:00");
            assertEquals(60, result1);
            
            // Test with extra spaces
            Integer result2 = (Integer) method.invoke(maintenanceService, "  10:00  -  11:00  ");
            assertEquals(60, result2);
            
            // Test with case where end time is less than start time (overnight booking)
            Integer result3 = (Integer) method.invoke(maintenanceService, "23:00 - 01:00");
            assertEquals(120, result3);
            
            // Test with very short duration (5 minutes)
            Integer result4 = (Integer) method.invoke(maintenanceService, "10:00 - 10:05");
            assertEquals(5, result4);
            
            // Test with very long duration (12 hours)
            Integer result5 = (Integer) method.invoke(maintenanceService, "07:00 - 19:00");
            assertEquals(720, result5);
            
            // Test with empty string
            Integer result6 = (Integer) method.invoke(maintenanceService, "");
            assertEquals(0, result6);
        } catch (Exception e) {
            fail("Failed to test calculateBookingCost method: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Cancel bookings should handle credit refund failure")
    void cancelBookingsShouldHandleCreditRefundFailure() throws MessagingException {
        // Mock facility repository
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(testFacility));
        
        // Mock successful email sending
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);
        
        // Mock booking repository save
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        
        // Mock credit service to fail
        when(creditService.addCredits(anyLong(), any(Integer.class))).thenReturn(false);
        
        // Call method
        int result = maintenanceService.cancelBookingsForMaintenance(
                affectedBookings, maintenanceSchedule, emailService);
        
        // Should still return 1 since email was sent successfully, even though credit refund failed
        assertEquals(1, result);
        
        // Verify booking was still cancelled
        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
        verify(bookingRepository).save(testBooking);
        
        // Verify credit service was called but failed
        verify(creditService).addCredits(eq(1L), any(Integer.class));
    }

    @Test
    @DisplayName("Check maintenance status batch should handle invalid facility ID types")
    void checkMaintenanceStatusBatchShouldHandleInvalidFacilityIDTypes() {
        // Create a list with valid and invalid ID types, but no null values
        List<Object> mixedIds = Arrays.asList(1L, 2, "3", "invalid", 0, -1);
        
        // Mock repository to return some facilities under maintenance
        when(maintenanceRepository.findFacilitiesUnderMaintenanceOnDate(anyList(), anyString()))
                .thenReturn(Arrays.asList(1L, 3L));
        
        // Call the method
        Map<String, Boolean> result = maintenanceService.checkMaintenanceStatusBatch(mixedIds, today);
        
        // Verify result
        assertNotNull(result);
        
        // Should filter out invalid IDs but still have valid ones
        assertTrue(result.containsKey("1"));
        assertTrue(result.containsKey("2"));
        assertTrue(result.containsKey("3"));
        assertTrue(result.containsKey("0"));
        assertTrue(result.containsKey("-1"));
        
        // Invalid IDs should be filtered out
        assertFalse(result.containsKey("invalid"));
        
        // Verify correct maintenance status
        assertTrue(result.get("1"));
        assertFalse(result.get("2"));
        assertTrue(result.get("3"));
        assertFalse(result.get("0"));
        assertFalse(result.get("-1"));
    }

    @Test
    @DisplayName("Check maintenance status batch should handle repository exceptions")
    void checkMaintenanceStatusBatchShouldHandleRepositoryExceptions() {
        // Create a list of IDs
        List<Long> facilityIds = Arrays.asList(1L, 2L, 3L);
        
        // Mock repository to throw exception
        when(maintenanceRepository.findFacilitiesUnderMaintenanceOnDate(anyList(), anyString()))
                .thenThrow(new RuntimeException("Database error"));
        
        // Call the method
        Map<String, Boolean> result = maintenanceService.checkMaintenanceStatusBatch(facilityIds, today);
        
        // Should return empty map on error
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // All facilities should be marked as not under maintenance when exception occurs
        assertFalse(result.get("1"));
        assertFalse(result.get("2"));
        assertFalse(result.get("3"));
    }

    @Test
    @DisplayName("Has overlapping maintenance should handle adjacent but non-overlapping periods")
    void hasOverlappingMaintenanceShouldHandleAdjacentButNonOverlappingPeriods() {
        // Create maintenance schedule ending on a specific date
        MaintenanceSchedule existingSchedule = MaintenanceSchedule.builder()
                .maintenanceId(2L)
                .facilityId(1L)
                .startDate(today.minusDays(10).toString())
                .endDate(today.toString()) // Ends today
                .build();
        
        // Create new maintenance starting on the next day (adjacent but not overlapping)
        String newStartDateStr = today.plusDays(1).toString();
        String newEndDateStr = today.plusDays(5).toString();
        
        // Mock repository
        when(maintenanceRepository.findByFacilityId(anyLong()))
                .thenReturn(Arrays.asList(existingSchedule));
        
        // Test for overlap - should return false
        boolean result = maintenanceService.hasOverlappingMaintenance(
                1L, newStartDateStr, newEndDateStr, null);
        
        // Verify no overlap is detected
        assertFalse(result);
    }

    @Test
    @DisplayName("Has overlapping maintenance should detect exact same date overlap")
    void hasOverlappingMaintenanceShouldDetectExactSameDateOverlap() {
        // Create maintenance schedule with specific dates
        MaintenanceSchedule existingSchedule = MaintenanceSchedule.builder()
                .maintenanceId(2L)
                .facilityId(1L)
                .startDate(today.toString())
                .endDate(today.plusDays(5).toString())
                .build();
        
        // Create new maintenance with exact same dates
        String newStartDateStr = today.toString();
        String newEndDateStr = today.plusDays(5).toString();
        
        // Mock repository
        when(maintenanceRepository.findByFacilityId(anyLong()))
                .thenReturn(Arrays.asList(existingSchedule));
        
        // Test for overlap - should return true for exact same dates
        boolean result = maintenanceService.hasOverlappingMaintenance(
                1L, newStartDateStr, newEndDateStr, null);
        
        // Verify overlap is detected
        assertTrue(result);
    }

    @Test
    @DisplayName("Find bookings affected by maintenance should handle null creation time")
    void findBookingsAffectedByMaintenanceShouldHandleNullCreationTime() {
        // Create test data
        String startDateStr = today.toString();
        String endDateStr = today.plusDays(5).toString();
        
        // Mock repository with expected parameters
        when(bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(affectedBookings);
        
        // Call method with null creation time
        List<Booking> result = maintenanceService.findBookingsAffectedByMaintenance(
                1L, startDateStr, endDateStr, null);
        
        // Should still work and use current time
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getBookingId());
        
        // Verify appropriate repository method was called
        verify(bookingRepository).findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), anyList());
    }

    @Test
    @DisplayName("Get maintenance for facility on date should handle null date parameter")
    void getMaintenanceForFacilityOnDateShouldHandleNullDateParameter() {
        // Create maintenance schedule
        MaintenanceSchedule currentSchedule = MaintenanceSchedule.builder()
                .maintenanceId(1L)
                .facilityId(1L)
                .startDate(today.minusDays(1).toString())
                .endDate(today.plusDays(1).toString()) // covers today
                .build();
        
        // Mock repository
        when(maintenanceRepository.findByFacilityId(anyLong()))
                .thenReturn(Arrays.asList(currentSchedule));
        
        // Call method with null date
        Optional<MaintenanceSchedule> result = maintenanceService.getMaintenanceForFacilityOnDate(1L, null);
        
        // Should use current date (today) and find the maintenance
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getMaintenanceId());
    }

    @Test
    @DisplayName("Is facility under maintenance on date should handle null date parameter")
    void isFacilityUnderMaintenanceOnDateShouldHandleNullDateParameter() {
        // Create maintenance schedule covering today
        MaintenanceSchedule currentSchedule = MaintenanceSchedule.builder()
                .maintenanceId(1L)
                .facilityId(1L)
                .startDate(today.minusDays(1).toString())
                .endDate(today.plusDays(1).toString()) // covers today
                .build();
        
        // Mock repository
        when(maintenanceRepository.findByFacilityId(anyLong()))
                .thenReturn(Arrays.asList(currentSchedule));
        
        // Call method with null date
        boolean result = maintenanceService.isFacilityUnderMaintenanceOnDate(1L, null);
        
        // Should use current date (today) and find facility under maintenance
        assertTrue(result);
    }

    @Test
    @DisplayName("Calculate booking cost should handle different time formats")
    void calculateBookingCostShouldHandleDifferentTimeFormats() {
        try {
            java.lang.reflect.Method method = MaintenanceService.class.getDeclaredMethod(
                    "calculateBookingCost", String.class);
            method.setAccessible(true);
            
            // Test 12-hour format with AM/PM
            Integer result1 = (Integer) method.invoke(maintenanceService, "10:00 AM - 11:00 AM");
            assertEquals(0, result1); // Returns 0 for unsupported format
            
            // Test format without leading zeros - apparently not supported
            Integer result2 = (Integer) method.invoke(maintenanceService, "9:00 - 10:00");
            assertEquals(0, result2); // Returns 0 for unsupported format
            
            // Test format with leading zeros (should work)
            Integer result3 = (Integer) method.invoke(maintenanceService, "09:00 - 10:00");
            assertEquals(60, result3);
            
            // Test format with irregular spacing
            Integer result4 = (Integer) method.invoke(maintenanceService, "10:00-   11:30");
            assertEquals(90, result4);
        } catch (Exception e) {
            fail("Failed to test calculateBookingCost method: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Check maintenance status batch should handle empty facilityIds list")
    void checkMaintenanceStatusBatchShouldHandleEmptyFacilityIdsList() {
        // Call method with empty list
        Map<String, Boolean> result = maintenanceService.checkMaintenanceStatusBatch(
                Collections.emptyList(), today);
        
        // Should return empty map
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        // Repository should not be called
        verify(maintenanceRepository, never()).findFacilitiesUnderMaintenanceOnDate(anyList(), anyString());
    }

    @Test
    @DisplayName("Check maintenance status batch should handle null facilityIds parameter")
    void checkMaintenanceStatusBatchShouldHandleNullFacilityIdsParameter() {
        // Call method with null
        Map<String, Boolean> result = maintenanceService.checkMaintenanceStatusBatch(null, today);
        
        // Should return empty map
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        // Repository should not be called
        verify(maintenanceRepository, never()).findFacilitiesUnderMaintenanceOnDate(anyList(), anyString());
    }

    @Test
    @DisplayName("Cancel bookings should handle facility not found")
    void cancelBookingsShouldHandleFacilityNotFound() throws MessagingException {
        // Mock facility repository to return empty
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // Call method
        int result = maintenanceService.cancelBookingsForMaintenance(
                affectedBookings, maintenanceSchedule, emailService);
        
        // Should return 0 since facility wasn't found
        assertEquals(0, result);
        
        // Verify booking was not saved
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Cancel bookings should handle booking save exception")
    void cancelBookingsShouldHandleBookingSaveException() throws MessagingException {
        // Mock facility repository
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(testFacility));
        
        // Mock booking repository save to throw exception
        when(bookingRepository.save(any(Booking.class))).thenThrow(new RuntimeException("Database error"));
        
        // Call method
        int result = maintenanceService.cancelBookingsForMaintenance(
                affectedBookings, maintenanceSchedule, emailService);
        
        // Should return 0 due to exception
        assertEquals(0, result);
        
        // Email should not be sent due to exception
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Cancel bookings should handle no credits to refund")
    void cancelBookingsShouldHandleNoCreditsToRefund() throws MessagingException {
        // Mock facility repository
        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(testFacility));
        
        // Create a booking with a time slot that would calculate to 0 credits
        Booking bookingWithInvalidTimeSlot = Booking.builder()
                .bookingId(1L)
                .facility(testFacility)
                .account(testAccount)
                .bookedDateTime(today.atTime(10, 0))
                .timeSlot("invalid") // Invalid time slot will result in 0 credits
                .status(BookingStatus.APPROVED)
                .build();
        
        // Mock booking repository save
        when(bookingRepository.save(any(Booking.class))).thenReturn(bookingWithInvalidTimeSlot);
        
        // Mock successful email sending
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);
        
        // Call method with booking that has invalid time slot
        int result = maintenanceService.cancelBookingsForMaintenance(
                Arrays.asList(bookingWithInvalidTimeSlot), maintenanceSchedule, emailService);
        
        // Should still return 1 since everything else succeeded
        assertEquals(1, result);
        
        // Verify credit service was not called due to 0 credits
        verify(creditService, never()).addCredits(anyLong(), any(Integer.class));
    }

    @Test
    @DisplayName("Has overlapping maintenance should handle empty existing schedules")
    void hasOverlappingMaintenanceShouldHandleEmptyExistingSchedules() {
        // Mock repository to return empty list
        when(maintenanceRepository.findByFacilityId(anyLong()))
                .thenReturn(Collections.emptyList());
        
        // Test for overlap with no existing schedules
        boolean result = maintenanceService.hasOverlappingMaintenance(
                1L, today.toString(), futureDate.toString(), null);
        
        // Should return false when no existing schedules
        assertFalse(result);
        verify(maintenanceRepository).findByFacilityId(1L);
    }
}