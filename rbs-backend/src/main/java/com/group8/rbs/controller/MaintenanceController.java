package com.group8.rbs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.MaintenanceSchedule;
import com.group8.rbs.repository.AccountRepository;
import com.group8.rbs.service.maintenance.MaintenanceService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/maintenance")
@CrossOrigin(origins = "*")
public class MaintenanceController {
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceController.class);
    private final MaintenanceService maintenanceService;
    private final AccountRepository accountRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public MaintenanceController(MaintenanceService maintenanceService, AccountRepository accountRepository) {
        this.maintenanceService = maintenanceService;
        this.accountRepository = accountRepository;
    }
    
    /**
     * Schedule a maintenance for a facility with validation
     */
    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleMaintenanceForFacility(@RequestBody Map<String, Object> requestMap) {
        try {
            logger.info("Maintenance request received: {}", requestMap);
            
            // Validation for required fields
            if (!requestMap.containsKey("facilityId")) {
                return createErrorResponse("Facility ID is required");
            }
            
            if (!requestMap.containsKey("startDate") || !requestMap.containsKey("endDate")) {
                return createErrorResponse("Start date and end date are required");
            }
            
            String startDateStr = (String) requestMap.get("startDate");
            String endDateStr = (String) requestMap.get("endDate");
            
            // Validate date formats
            LocalDate startDate;
            LocalDate endDate;
            
            try {
                startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
                endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                return createErrorResponse("Invalid date format. Please use YYYY-MM-DD format");
            }
            
            // Validate start date is not after end date
            if (startDate.isAfter(endDate)) {
                return createErrorResponse("Start date cannot be after end date");
            }
            
            // Validate start date is not before today
            if (startDate.isBefore(LocalDate.now())) {
                return createErrorResponse("Start date cannot be in the past");
            }
            
            // Get the facility ID
            Long facilityId = Long.valueOf(requestMap.get("facilityId").toString());
            
            // Check if the facility is already under maintenance
            if (maintenanceService.isFacilityUnderMaintenance(facilityId)) {
                return createErrorResponse("This facility is already under maintenance");
            }
            
            // Create a new maintenance schedule
            MaintenanceSchedule maintenanceSchedule = new MaintenanceSchedule();
            
            // Set facility ID
            maintenanceSchedule.setFacilityId(facilityId);
            
            // Set description
            if (requestMap.containsKey("description")) {
                maintenanceSchedule.setDescription((String) requestMap.get("description"));
            }
            
            // Set dates
            maintenanceSchedule.setStartDate(startDateStr);
            maintenanceSchedule.setEndDate(endDateStr);
            
            // Handle createdBy email conversion to ID
            Long createdById = 1L; // Default admin ID
            
            if (requestMap.containsKey("createdBy")) {
                String createdByEmail = (String) requestMap.get("createdBy");
                
                if (createdByEmail != null && createdByEmail.contains("@")) {
                    // Look up the account by email
                    Optional<Account> account = accountRepository.findByEmail(createdByEmail);
                    if (account.isPresent()) {
                        createdById = account.get().getAccountId();
                        logger.info("Found account ID {} for email {}", createdById, createdByEmail);
                    } else {
                        logger.warn("No account found for email: {}, using default admin ID", createdByEmail);
                    }
                }
            }
            
            // Set the created by ID (as Long)
            maintenanceSchedule.setCreatedBy(createdById);
            
            // Save the maintenance schedule
            MaintenanceSchedule savedSchedule = maintenanceService.scheduleMaintenanceForFacility(maintenanceSchedule);
            
            return new ResponseEntity<>(savedSchedule, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating maintenance schedule", e);
            return createErrorResponse("An error occurred: " + e.getMessage());
        }
    }
    
    // Helper method to create error responses
    private ResponseEntity<Map<String, String>> createErrorResponse(String errorMessage) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", errorMessage);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Get all maintenance schedules
     */
    @GetMapping("/all")
    public ResponseEntity<List<MaintenanceSchedule>> getAllMaintenanceSchedules() {
        logger.info("Fetching all maintenance schedules");
        List<MaintenanceSchedule> schedules = maintenanceService.getAllMaintenanceSchedules();
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    /**
     * Get maintenance schedules for a specific facility
     */
    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<List<MaintenanceSchedule>> getMaintenanceSchedulesForFacility(@PathVariable Long facilityId) {
        logger.info("Fetching maintenance schedules for facility ID: {}", facilityId);
        List<MaintenanceSchedule> schedules = maintenanceService.getMaintenanceSchedulesForFacility(facilityId);
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    /**
     * Get a specific maintenance schedule by ID
     */
    @GetMapping("/{maintenanceId}")
    public ResponseEntity<Object> getMaintenanceScheduleById(@PathVariable Long maintenanceId) {
        logger.info("Fetching maintenance schedule with ID: {}", maintenanceId);
        return maintenanceService.getMaintenanceScheduleById(maintenanceId)
            .map(schedule -> new ResponseEntity<Object>(schedule, HttpStatus.OK))
            .orElse(new ResponseEntity<Object>("Maintenance schedule not found", HttpStatus.NOT_FOUND));
    }
    
    /**
     * Update an existing maintenance schedule
     */
    @PutMapping("/update/{maintenanceId}")
    public ResponseEntity<Object> updateMaintenanceSchedule(
            @PathVariable Long maintenanceId, 
            @RequestBody MaintenanceSchedule maintenanceSchedule) {
        logger.info("Updating maintenance schedule with ID: {}", maintenanceId);
        return maintenanceService.getMaintenanceScheduleById(maintenanceId)
            .map(existingSchedule -> {
                maintenanceSchedule.setMaintenanceId(maintenanceId);
                MaintenanceSchedule updatedSchedule = maintenanceService.updateMaintenanceSchedule(maintenanceSchedule);
                return new ResponseEntity<Object>(updatedSchedule, HttpStatus.OK);
            })
            .orElse(new ResponseEntity<Object>("Maintenance schedule not found", HttpStatus.NOT_FOUND));
    }
    
    /**
     * Delete a maintenance schedule
     */
    @DeleteMapping("/delete/{maintenanceId}")
    public ResponseEntity<String> deleteMaintenanceSchedule(@PathVariable Long maintenanceId) {
        logger.info("Deleting maintenance schedule with ID: {}", maintenanceId);
        if (maintenanceService.getMaintenanceScheduleById(maintenanceId).isPresent()) {
            maintenanceService.deleteMaintenanceSchedule(maintenanceId);
            return new ResponseEntity<>("Maintenance schedule deleted successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Maintenance schedule not found", HttpStatus.NOT_FOUND);
        }
    }
    
    /**
     * Check if a facility is currently under maintenance
     */
    @GetMapping("/check/{facilityId}")
    public ResponseEntity<Boolean> isFacilityUnderMaintenance(@PathVariable Long facilityId) {
        logger.info("Checking if facility ID: {} is under maintenance", facilityId);
        boolean isUnderMaintenance = maintenanceService.isFacilityUnderMaintenance(facilityId);
        return new ResponseEntity<>(isUnderMaintenance, HttpStatus.OK);
    }
}