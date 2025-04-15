package com.group8.rbs.service.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group8.rbs.entities.Booking;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.entities.MaintenanceSchedule;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.repository.BookingRepository;
import com.group8.rbs.repository.FacilityRepository;
import com.group8.rbs.repository.MaintenanceRepository;
import com.group8.rbs.exception.MaintenanceOverlapException;
import com.group8.rbs.service.email.CustomEmailService;

import jakarta.mail.MessagingException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MaintenanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceService.class);
    private final MaintenanceRepository maintenanceRepository;
    private final BookingRepository bookingRepository;
    private final FacilityRepository facilityRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Autowired
    public MaintenanceService(
            MaintenanceRepository maintenanceRepository,
            BookingRepository bookingRepository,
            FacilityRepository facilityRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.bookingRepository = bookingRepository;
        this.facilityRepository = facilityRepository;
    }
    
    /**
     * Find bookings affected by a planned maintenance
     */
    public List<Booking> findBookingsAffectedByMaintenance(Long facilityId, String startDateStr, String endDateStr) {
        LocalDate startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
        
        // Convert LocalDate to LocalDateTime for the start of the day
        LocalDateTime startDateTime = startDate.atStartOfDay();
        // Convert LocalDate to LocalDateTime for the end of the day
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        // Find bookings within the date range that are approved or pending
        List<BookingStatus> statuses = List.of(BookingStatus.APPROVED, BookingStatus.CONFIRMED, BookingStatus.PENDING);
        
        return bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
            facilityId, startDateTime, endDateTime, statuses);
    }
    
    /**
     * Cancel bookings affected by maintenance and send email notifications
     * @return Number of bookings cancelled
     */
    @Transactional
    public int cancelBookingsForMaintenance(
            List<Booking> bookings, 
            MaintenanceSchedule maintenance,
            CustomEmailService emailService) {
        
        if (bookings == null || bookings.isEmpty()) {
            return 0;
        }
        
        // Get facility information for the email
        Optional<Facility> facilityOpt = facilityRepository.findById(maintenance.getFacilityId());
        if (!facilityOpt.isPresent()) {
            logger.error("Facility not found for ID: {}", maintenance.getFacilityId());
            return 0;
        }
        
        Facility facility = facilityOpt.get();
        int cancelledCount = 0;
        
        for (Booking booking : bookings) {
            try {
                // Update booking status to CANCELLED
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
                
                // Send email notification
                String toEmail = booking.getAccount().getEmail();
                String subject = "Booking Cancelled Due to Maintenance";
                
                String body = "<html>" +
                    "<body>" +
                    "<p>Dear " + booking.getAccount().getName() + ",</p>" +
                    "<p>We regret to inform you that your booking has been cancelled due to scheduled maintenance.</p>" +
                    "<p><strong>Booking ID:</strong> " + booking.getBookingId() + "</p>" +
                    "<p><strong>Facility:</strong> " + facility.getResourceName() + "</p>" +
                    "<p><strong>Location:</strong> " + facility.getLocation() + "</p>" +
                    "<p><strong>Date:</strong> " + booking.getBookedDateTime().toLocalDate() + "</p>" +
                    "<p><strong>Time Slot:</strong> " + booking.getTimeSlot() + "</p>" +
                    "<p><strong>Maintenance Period:</strong> " + maintenance.getStartDate() + " to " + maintenance.getEndDate() + "</p>" +
                    "<p>Maintenance Details: " + maintenance.getDescription() + "</p>" +
                    "<p>We apologize for any inconvenience this may cause. Please feel free to make a new booking for a different time or facility.</p>" +
                    "<br>" +
                    "<p>Best regards,</p>" +
                    "<p>Resource Booking System</p>" +
                    "</body>" +
                    "</html>";
                
                boolean emailSent = emailService.sendEmail(toEmail, subject, body);
                
                if (emailSent) {
                    logger.info("Cancellation email sent successfully to {}", toEmail);
                    cancelledCount++;
                } else {
                    logger.warn("Failed to send cancellation email to {}", toEmail);
                }
                
            } catch (MessagingException e) {
                logger.error("Error sending cancellation email for booking ID {}: {}", booking.getBookingId(), e.getMessage());
            } catch (Exception e) {
                logger.error("Error cancelling booking ID {}: {}", booking.getBookingId(), e.getMessage());
            }
        }
        
        return cancelledCount;
    }
    
    /**
     * Check if there is any overlapping maintenance for the specified facility and dates
     */
    public boolean hasOverlappingMaintenance(Long facilityId, String startDateStr, String endDateStr, Long excludeMaintenanceId) {
        LocalDate startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
        
        List<MaintenanceSchedule> existingSchedules = maintenanceRepository.findByFacilityId(facilityId);
        
        for (MaintenanceSchedule schedule : existingSchedules) {
            // Skip the current maintenance schedule being updated (if any)
            if (excludeMaintenanceId != null && schedule.getMaintenanceId().equals(excludeMaintenanceId)) {
                continue;
            }
            
            LocalDate scheduleStartDate = LocalDate.parse(schedule.getStartDate(), DATE_FORMATTER);
            LocalDate scheduleEndDate = LocalDate.parse(schedule.getEndDate(), DATE_FORMATTER);
            
            // Check for overlap
            // Two date ranges overlap if one range starts before the other ends and ends after the other starts
            boolean overlap = startDate.isBefore(scheduleEndDate.plusDays(1)) && 
                              endDate.isAfter(scheduleStartDate.minusDays(1));
                              
            if (overlap) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Schedule a new maintenance for a facility
     */
    public MaintenanceSchedule scheduleMaintenanceForFacility(MaintenanceSchedule maintenanceSchedule) {
        // Check for overlapping maintenance
        if (hasOverlappingMaintenance(
                maintenanceSchedule.getFacilityId(), 
                maintenanceSchedule.getStartDate(), 
                maintenanceSchedule.getEndDate(),
                maintenanceSchedule.getMaintenanceId())) {
            throw new MaintenanceOverlapException("This facility already has scheduled maintenance during the selected dates");
        }
        
        return maintenanceRepository.save(maintenanceSchedule);
    }
    
    /**
     * Update the end date of a maintenance schedule (for early release)
     */
    public MaintenanceSchedule updateMaintenanceEndDate(Long maintenanceId, String newEndDate) {
        Optional<MaintenanceSchedule> scheduleOpt = maintenanceRepository.findById(maintenanceId);
        
        if (scheduleOpt.isPresent()) {
            MaintenanceSchedule schedule = scheduleOpt.get();
            schedule.setEndDate(newEndDate);
            return maintenanceRepository.save(schedule);
        } else {
            throw new RuntimeException("Maintenance schedule not found");
        }
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
        // Check for overlapping maintenance
        if (hasOverlappingMaintenance(
                maintenanceSchedule.getFacilityId(), 
                maintenanceSchedule.getStartDate(), 
                maintenanceSchedule.getEndDate(),
                maintenanceSchedule.getMaintenanceId())) {
            throw new MaintenanceOverlapException("This facility already has scheduled maintenance during the selected dates");
        }
        
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
        return maintenanceRepository.existsByFacilityIdAndCurrentDate(facilityId);
    }
    
    /**
     * Get active maintenance schedules (currently ongoing)
     */
    public List<MaintenanceSchedule> getActiveMaintenance() {
        return maintenanceRepository.findActiveMaintenance();
    }
    
    /**
     * Get upcoming maintenance schedules (future dates)
     */
    public List<MaintenanceSchedule> getUpcomingMaintenance() {
        return maintenanceRepository.findUpcomingMaintenance();
    }
    
    /**
     * Check if a facility is under maintenance on a specific date
     */
    public boolean isFacilityUnderMaintenanceOnDate(Long facilityId, LocalDate date) {
        // If no date provided, use current date
        LocalDate checkDate = date != null ? date : LocalDate.now();
        
        // Get all maintenance schedules for this facility
        List<MaintenanceSchedule> schedules = maintenanceRepository.findByFacilityId(facilityId);
        
        // Check if any maintenance schedule covers this date
        for (MaintenanceSchedule schedule : schedules) {
            LocalDate startDate = LocalDate.parse(schedule.getStartDate(), DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(schedule.getEndDate(), DATE_FORMATTER);
            
            // Check if the given date falls within this maintenance period
            if ((checkDate.isEqual(startDate) || checkDate.isAfter(startDate)) && 
                (checkDate.isEqual(endDate) || checkDate.isBefore(endDate))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get current active maintenance for a facility
     */
    public Optional<MaintenanceSchedule> getCurrentMaintenanceForFacility(Long facilityId) {
        List<MaintenanceSchedule> schedules = maintenanceRepository.findActiveMaintenance();
        return schedules.stream()
                .filter(schedule -> schedule.getFacilityId().equals(facilityId))
                .findFirst();
    }
    
    /**
     * Get maintenance schedule covering a specific date for a facility
     */
    public Optional<MaintenanceSchedule> getMaintenanceForFacilityOnDate(Long facilityId, LocalDate date) {
        // If no date provided, use current date
        LocalDate checkDate = date != null ? date : LocalDate.now();
        
        // Get all maintenance schedules for this facility
        List<MaintenanceSchedule> schedules = maintenanceRepository.findByFacilityId(facilityId);
        
        // Find the maintenance schedule that covers this date
        return schedules.stream()
                .filter(schedule -> {
                    LocalDate startDate = LocalDate.parse(schedule.getStartDate(), DATE_FORMATTER);
                    LocalDate endDate = LocalDate.parse(schedule.getEndDate(), DATE_FORMATTER);
                    
                    return (checkDate.isEqual(startDate) || checkDate.isAfter(startDate)) && 
                           (checkDate.isEqual(endDate) || checkDate.isBefore(endDate));
                })
                .findFirst();
    }
    
    /**
     * Check maintenance status for multiple facilities in a single operation
     * This reduces the number of database calls when checking maintenance for multiple facilities
     * @param facilityIds List of facility IDs to check
     * @param date The date to check maintenance for (null for current date)
     * @return Map of facility ID -> maintenance status
     */
    public Map<String, Boolean> checkMaintenanceStatusBatch(List<?> facilityIds, LocalDate date) {
        // Create a result map with all facilities initially set to not under maintenance
        Map<String, Boolean> result = new HashMap<>();
        
        // If no facility IDs provided, return empty map
        if (facilityIds == null || facilityIds.isEmpty()) {
            logger.info("No facility IDs provided for maintenance check");
            return result;
        }
        
        // Convert the input IDs to a list of Long values
        List<Long> longFacilityIds = new ArrayList<>();
        
        // Initialize all facilities to false (not under maintenance)
        for (Object idObj : facilityIds) {
            Long facilityId;
            
            // Handle different ID types (Integer, Long, String)
            if (idObj instanceof Integer) {
                facilityId = ((Integer) idObj).longValue();
            } else if (idObj instanceof Long) {
                facilityId = (Long) idObj;
            } else {
                try {
                    facilityId = Long.parseLong(idObj.toString());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid facility ID format: {}. Skipping.", idObj);
                    continue;
                }
            }
            
            longFacilityIds.add(facilityId);
            // Initialize this facility as not under maintenance
            result.put(facilityId.toString(), false);
        }
        
        // If no valid facility IDs found, return empty result
        if (longFacilityIds.isEmpty()) {
            logger.warn("No valid facility IDs found after conversion");
            return result;
        }
        
        // Use current date if none provided
        LocalDate checkDate = date != null ? date : LocalDate.now();
        String checkDateString = checkDate.toString(); // Converts to "yyyy-MM-dd"
        logger.info("Checking maintenance status for {} facilities on date: {}", 
                    longFacilityIds.size(), checkDate);
        
        try {
            // Use the optimized repository method to get only facilities under maintenance
            List<Long> facilitiesUnderMaintenance = maintenanceRepository
                .findFacilitiesUnderMaintenanceOnDate(longFacilityIds, checkDateString);

            logger.info("Facilities under maintenance on date {}: {}", checkDate, facilitiesUnderMaintenance);
            
            logger.info("Found {} facilities under maintenance on date {}", 
                    facilitiesUnderMaintenance.size(), checkDate);
            
            // Update the result map with facilities that are under maintenance
            for (Long facilityId : facilitiesUnderMaintenance) {
                result.put(facilityId.toString(), true);
                logger.info("Facility ID {} is under maintenance on date {}", facilityId, checkDate);
            }
        } catch (Exception e) {
            logger.error("Error checking maintenance status for facilities: {}", e.getMessage(), e);
        }
        
        return result;
    }
}