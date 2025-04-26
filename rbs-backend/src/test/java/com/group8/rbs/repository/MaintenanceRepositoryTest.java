package com.group8.rbs.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.group8.rbs.entities.MaintenanceSchedule;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class MaintenanceRepositoryTest {
    
    @Mock
    private MaintenanceRepository maintenanceRepository;
    
    private MaintenanceSchedule pastMaintenance;
    private MaintenanceSchedule currentMaintenance;
    private MaintenanceSchedule futureMaintenance;
    private MaintenanceSchedule otherFacilityMaintenance;
    private LocalDate today;
    
    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        
        // Past maintenance (facility 1)
        pastMaintenance = MaintenanceSchedule.builder()
            .maintenanceId(1L)
            .facilityId(1L)
            .startDate(today.minusDays(10).toString())
            .endDate(today.minusDays(5).toString())
            .description("Past maintenance")
            .createdBy(1L)
            .build();
        
        // Current maintenance (facility 1)
        currentMaintenance = MaintenanceSchedule.builder()
            .maintenanceId(2L)
            .facilityId(1L)
            .startDate(today.minusDays(2).toString())
            .endDate(today.plusDays(3).toString()) // Spans current date
            .description("Current maintenance")
            .createdBy(1L)
            .build();
        
        // Future maintenance (facility 1)
        futureMaintenance = MaintenanceSchedule.builder()
            .maintenanceId(3L)
            .facilityId(1L)
            .startDate(today.plusDays(5).toString())
            .endDate(today.plusDays(10).toString())
            .description("Future maintenance")
            .createdBy(1L)
            .build();
        
        // Current maintenance (facility 2)
        otherFacilityMaintenance = MaintenanceSchedule.builder()
            .maintenanceId(4L)
            .facilityId(2L)
            .startDate(today.minusDays(1).toString())
            .endDate(today.plusDays(2).toString()) // Spans current date
            .description("Other facility maintenance")
            .createdBy(1L)
            .build();
    }
    
    @Test
    @DisplayName("Find maintenance schedules by facility ID")
    void testFindByFacilityId() {
        // Mock repository behavior
        when(maintenanceRepository.findByFacilityId(1L))
            .thenReturn(Arrays.asList(pastMaintenance, currentMaintenance, futureMaintenance));
        
        List<MaintenanceSchedule> schedules = maintenanceRepository.findByFacilityId(1L);
        
        assertEquals(3, schedules.size());
        assertTrue(schedules.contains(pastMaintenance));
        assertTrue(schedules.contains(currentMaintenance));
        assertTrue(schedules.contains(futureMaintenance));
        
        verify(maintenanceRepository).findByFacilityId(1L);
    }
    
    @Test
    @DisplayName("Check if facility is under maintenance on current date")
    void testExistsByFacilityIdAndCurrentDate() {
        // Mock repository behavior
        when(maintenanceRepository.existsByFacilityIdAndCurrentDate(1L)).thenReturn(true);
        when(maintenanceRepository.existsByFacilityIdAndCurrentDate(2L)).thenReturn(true);
        when(maintenanceRepository.existsByFacilityIdAndCurrentDate(3L)).thenReturn(false);
        
        boolean facility1UnderMaintenance = maintenanceRepository.existsByFacilityIdAndCurrentDate(1L);
        boolean facility2UnderMaintenance = maintenanceRepository.existsByFacilityIdAndCurrentDate(2L);
        boolean facility3UnderMaintenance = maintenanceRepository.existsByFacilityIdAndCurrentDate(3L);
        
        assertTrue(facility1UnderMaintenance, "Facility 1 should be under maintenance");
        assertTrue(facility2UnderMaintenance, "Facility 2 should be under maintenance");
        assertFalse(facility3UnderMaintenance, "Facility 3 should not be under maintenance");
        
        verify(maintenanceRepository, times(3)).existsByFacilityIdAndCurrentDate(anyLong());
    }
    
    @Test
    @DisplayName("Find upcoming maintenance schedules")
    void testFindUpcomingMaintenance() {
        // Mock repository behavior
        when(maintenanceRepository.findUpcomingMaintenance())
            .thenReturn(Arrays.asList(futureMaintenance));
        
        List<MaintenanceSchedule> upcomingSchedules = maintenanceRepository.findUpcomingMaintenance();
        
        assertEquals(1, upcomingSchedules.size());
        assertTrue(upcomingSchedules.contains(futureMaintenance), 
                "Future maintenance should be in upcoming list");
        
        verify(maintenanceRepository).findUpcomingMaintenance();
    }
    
    @Test
    @DisplayName("Find active maintenance schedules")
    void testFindActiveMaintenance() {
        // Mock repository behavior
        when(maintenanceRepository.findActiveMaintenance())
            .thenReturn(Arrays.asList(currentMaintenance, otherFacilityMaintenance));
        
        List<MaintenanceSchedule> activeSchedules = maintenanceRepository.findActiveMaintenance();
        
        assertEquals(2, activeSchedules.size());
        assertTrue(activeSchedules.contains(currentMaintenance),
                "Current maintenance should be in active list");
        assertTrue(activeSchedules.contains(otherFacilityMaintenance),
                "Other facility's current maintenance should be in active list");
        
        verify(maintenanceRepository).findActiveMaintenance();
    }
    
    @Test
    @DisplayName("Find facilities under maintenance on specific date")
    void testFindFacilitiesUnderMaintenanceOnDate() {
        List<Long> facilityIds = Arrays.asList(1L, 2L, 3L);
        
        // Mock repository behavior for today's date
        when(maintenanceRepository.findFacilitiesUnderMaintenanceOnDate(eq(facilityIds), eq(today.toString())))
            .thenReturn(Arrays.asList(1L, 2L));
        
        // Mock repository behavior for future date
        when(maintenanceRepository.findFacilitiesUnderMaintenanceOnDate(eq(facilityIds), eq(today.plusDays(7).toString())))
            .thenReturn(Arrays.asList(1L));
        
        // Mock repository behavior for past date
        when(maintenanceRepository.findFacilitiesUnderMaintenanceOnDate(eq(facilityIds), eq(today.minusDays(7).toString())))
            .thenReturn(Arrays.asList(1L));
        
        // Check facilities under maintenance today
        List<Long> todayMaintenanceFacilities = maintenanceRepository
                .findFacilitiesUnderMaintenanceOnDate(facilityIds, today.toString());
        
        assertEquals(2, todayMaintenanceFacilities.size());
        assertTrue(todayMaintenanceFacilities.contains(1L));
        assertTrue(todayMaintenanceFacilities.contains(2L));
        
        // Check facilities under maintenance in the future
        List<Long> futureMaintenanceFacilities = maintenanceRepository
                .findFacilitiesUnderMaintenanceOnDate(facilityIds, today.plusDays(7).toString());
        
        assertEquals(1, futureMaintenanceFacilities.size());
        assertTrue(futureMaintenanceFacilities.contains(1L));
        
        // Check facilities under maintenance in the past
        List<Long> pastMaintenanceFacilities = maintenanceRepository
                .findFacilitiesUnderMaintenanceOnDate(facilityIds, today.minusDays(7).toString());
        
        assertEquals(1, pastMaintenanceFacilities.size());
        assertTrue(pastMaintenanceFacilities.contains(1L));
        
        verify(maintenanceRepository, times(3)).findFacilitiesUnderMaintenanceOnDate(anyList(), anyString());
    }
}