package com.group8.rbs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.entities.MaintenanceSchedule;
import com.group8.rbs.exception.MaintenanceOverlapException;
import com.group8.rbs.repository.AccountRepository;
import com.group8.rbs.service.maintenance.MaintenanceService;
import com.group8.rbs.service.email.CustomEmailService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/maintenance")
@CrossOrigin(origins = "*")
public class MaintenanceController {
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceController.class);
    private final MaintenanceService maintenanceService;
    private final AccountRepository accountRepository;
    private final CustomEmailService emailService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public MaintenanceController(MaintenanceService maintenanceService, 
                                AccountRepository accountRepository,
                                CustomEmailService emailService) {
        this.maintenanceService = maintenanceService;
        this.accountRepository = accountRepository;
        this.emailService = emailService;
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
            
            // Get the current time for booking cancellation logic
            LocalDateTime creationTime = LocalDateTime.now();
            
            // Check for affected bookings - using the current time as the cutoff
            List<Booking> affectedBookings = maintenanceService.findBookingsAffectedByMaintenance(
                facilityId, startDateStr, endDateStr, creationTime);
            
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
            
            // If there are affected bookings, cancel them and send emails
            if (!affectedBookings.isEmpty()) {
                int cancelledCount = maintenanceService.cancelBookingsForMaintenance(
                    affectedBookings, savedSchedule, emailService);
                
                // Add information about cancelled bookings to the response
                Map<String, Object> response = new HashMap<>();
                response.put("maintenanceSchedule", savedSchedule);
                response.put("cancelledBookings", cancelledCount);
                
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            }
            
            return new ResponseEntity<>(savedSchedule, HttpStatus.CREATED);
        } catch (MaintenanceOverlapException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating maintenance schedule", e);
            return createErrorResponse("An error occurred: " + e.getMessage());
        }
    }
    
    /**
     * Get affected bookings count for a facility maintenance period
     */
    @GetMapping("/affected-bookings")
    public ResponseEntity<?> getAffectedBookingsCount(
            @RequestParam Long facilityId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            // Use current time as the reference point for cancellations
            LocalDateTime now = LocalDateTime.now();
            
            List<Booking> affectedBookings = maintenanceService.findBookingsAffectedByMaintenance(
                facilityId, startDate, endDate, now);
            
            Map<String, Object> response = new HashMap<>();
            response.put("count", affectedBookings.size());
            response.put("bookings", affectedBookings);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching affected bookings", e);
            return createErrorResponse("Failed to get affected bookings: " + e.getMessage());
        }
    }
    
    /**
     * Release a facility from maintenance early
     */
    @PostMapping("/release/{facilityId}")
    public ResponseEntity<?> releaseFacilityFromMaintenance(@PathVariable Long facilityId) {
        try {
            // Get current maintenance for facility
            Optional<MaintenanceSchedule> currentMaintenance = maintenanceService.getCurrentMaintenanceForFacility(facilityId);
            
            if (!currentMaintenance.isPresent()) {
                return createErrorResponse("This facility is not currently under maintenance");
            }
            
            // Get today's date
            String today = LocalDate.now().format(DATE_FORMATTER);
            MaintenanceSchedule schedule = currentMaintenance.get();
            
            // Check if end date is already today
            if (schedule.getEndDate().equals(today)) {
                return createErrorResponse("Facility maintenance is already ending today");
            }
            
            // Update end date to today
            MaintenanceSchedule updatedSchedule = maintenanceService.updateMaintenanceEndDate(schedule.getMaintenanceId(), today);
            
            return new ResponseEntity<>(updatedSchedule, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error releasing facility from maintenance", e);
            return createErrorResponse("Failed to release facility: " + e.getMessage());
        }
    }
    
    /**
     * Get current maintenance for a facility
     */
    @GetMapping("/current/{facilityId}")
    public ResponseEntity<?> getCurrentMaintenanceForFacility(
            @PathVariable Long facilityId,
            @RequestParam(required = false) String date) {
        try {
            Optional<MaintenanceSchedule> maintenanceSchedule;
            
            // If date parameter provided, check maintenance for that date
            if (date != null && !date.isEmpty()) {
                try {
                    LocalDate checkDate = LocalDate.parse(date);
                    maintenanceSchedule = maintenanceService.getMaintenanceForFacilityOnDate(facilityId, checkDate);
                } catch (DateTimeParseException e) {
                    logger.warn("Invalid date format: {}, using current date", date);
                    maintenanceSchedule = maintenanceService.getCurrentMaintenanceForFacility(facilityId);
                }
            } else {
                // Default to current date
                maintenanceSchedule = maintenanceService.getCurrentMaintenanceForFacility(facilityId);
            }
            
            if (maintenanceSchedule.isPresent()) {
                return new ResponseEntity<>(maintenanceSchedule.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.error("Error getting current maintenance", e);
            return createErrorResponse("Failed to get current maintenance: " + e.getMessage());
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
     * Check if a facility is currently under maintenance
     */
    @GetMapping("/check/{facilityId}")
    public ResponseEntity<Boolean> isFacilityUnderMaintenance(
            @PathVariable Long facilityId,
            @RequestParam(required = false) String date) {
        
        boolean isUnderMaintenance;
        
        // If date parameter provided, check for that specific date
        if (date != null && !date.isEmpty()) {
            try {
                LocalDate checkDate = LocalDate.parse(date);
                logger.info("Checking if facility ID: {} is under maintenance on date: {}", facilityId, checkDate);
                isUnderMaintenance = maintenanceService.isFacilityUnderMaintenanceOnDate(facilityId, checkDate);
            } catch (DateTimeParseException e) {
                logger.warn("Invalid date format: {}, using current date", date);
                isUnderMaintenance = maintenanceService.isFacilityUnderMaintenance(facilityId);
            }
        } else {
            // Default to current date check
            logger.info("Checking if facility ID: {} is under maintenance (current date)", facilityId);
            isUnderMaintenance = maintenanceService.isFacilityUnderMaintenance(facilityId);
        }
        
        return new ResponseEntity<>(isUnderMaintenance, HttpStatus.OK);
    }
    
    /**
     * Batch check for multiple facilities under maintenance for a specific date
     * This reduces the number of API calls needed when checking maintenance status
     */
    @PostMapping("/check-maintenance-status")
    public ResponseEntity<Map<String, Boolean>> checkMultipleFacilitiesMaintenance(
            @RequestBody Map<String, Object> requestMap) {
        
        @SuppressWarnings("unchecked")
        List<Long> facilityIds = (List<Long>) requestMap.get("facilityIds");
        String dateStr = (String) requestMap.get("date");
        
        logger.info("Checking maintenance status for {} facilities on date {}", 
                facilityIds != null ? facilityIds.size() : 0, dateStr);
        
        // Parse the date or use current date if not provided
        LocalDate checkDate = null;
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                checkDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                logger.warn("Invalid date format: {}. Using current date instead.", dateStr);
            }
        }
        
        // If facilityIds is null or empty, return empty map
        if (facilityIds == null || facilityIds.isEmpty()) {
            return new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
        }
        
        // Deduplicate facility IDs
        List<Long> uniqueFacilityIds = facilityIds.stream().distinct().collect(Collectors.toList());
        
        // Get maintenance status for all facilities in a single operation
        Map<String, Boolean> maintenanceStatusMap = maintenanceService.checkMaintenanceStatusBatch(uniqueFacilityIds, checkDate);
        
        return new ResponseEntity<>(maintenanceStatusMap, HttpStatus.OK);
    }
}