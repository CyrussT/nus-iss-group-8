package com.group8.rbs.service.maintenance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.group8.rbs.entities.MaintenanceSchedule;
import com.group8.rbs.repository.MaintenanceRepository;

import java.util.List;
import java.util.Optional;

@Service
public class MaintenanceService {
    
    private final MaintenanceRepository maintenanceRepository;
    
    @Autowired
    public MaintenanceService(MaintenanceRepository maintenanceRepository) {
        this.maintenanceRepository = maintenanceRepository;
    }
    
    /**
     * Schedule a new maintenance for a facility
     * The createdBy field should be properly set with a Long value before calling this method
     */
    public MaintenanceSchedule scheduleMaintenanceForFacility(MaintenanceSchedule maintenanceSchedule) {
        return maintenanceRepository.save(maintenanceSchedule);
    }
    
    /**
     * Get all maintenance schedules
     */
    public List<MaintenanceSchedule> getAllMaintenanceSchedules() {
        return maintenanceRepository.findAll();
    }
    
    /**
     * Get maintenance schedules for a specific facility
     */
    public List<MaintenanceSchedule> getMaintenanceSchedulesForFacility(Long facilityId) {
        return maintenanceRepository.findByFacilityId(facilityId);
    }
    
    /**
     * Get a specific maintenance schedule by ID
     */
    public Optional<MaintenanceSchedule> getMaintenanceScheduleById(Long maintenanceId) {
        return maintenanceRepository.findById(maintenanceId);
    }
    
    /**
     * Update an existing maintenance schedule
     */
    public MaintenanceSchedule updateMaintenanceSchedule(MaintenanceSchedule maintenanceSchedule) {
        return maintenanceRepository.save(maintenanceSchedule);
    }
    
    /**
     * Delete a maintenance schedule
     */
    public void deleteMaintenanceSchedule(Long maintenanceId) {
        maintenanceRepository.deleteById(maintenanceId);
    }
    
    /**
     * Check if a facility is currently under maintenance
     */
    public boolean isFacilityUnderMaintenance(Long facilityId) {
        // This would need a custom query to check current date against schedules
        return maintenanceRepository.existsByFacilityIdAndCurrentDate(facilityId);
    }
}