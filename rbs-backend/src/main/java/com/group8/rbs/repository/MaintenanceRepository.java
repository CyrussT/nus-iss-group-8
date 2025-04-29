package com.group8.rbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.group8.rbs.entities.MaintenanceSchedule;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceSchedule, Long> {
    
    /**
     * Find all maintenance schedules for a specific facility
     */
    List<MaintenanceSchedule> findByFacilityId(Long facilityId);
    
    /**
     * Check if a facility is currently under maintenance
     * Uses a custom query to compare current date with start and end dates
     */
    @Query(value = "SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MaintenanceSchedule m " +
           "WHERE m.facilityId = :facilityId " +
           "AND FUNCTION('date', CURRENT_TIMESTAMP) BETWEEN FUNCTION('date', m.startDate) AND FUNCTION('date', m.endDate)")
    boolean existsByFacilityIdAndCurrentDate(@Param("facilityId") Long facilityId);
    
    /**
     * Find upcoming maintenance schedules
     */
    @Query(value = "SELECT m FROM MaintenanceSchedule m " +
           "WHERE FUNCTION('date', m.startDate) > FUNCTION('date', CURRENT_TIMESTAMP) " +
           "ORDER BY m.startDate ASC")
    List<MaintenanceSchedule> findUpcomingMaintenance();
    
    /**
     * Find active maintenance schedules
     */
    @Query(value = "SELECT m FROM MaintenanceSchedule m " +
           "WHERE FUNCTION('date', CURRENT_TIMESTAMP) BETWEEN FUNCTION('date', m.startDate) AND FUNCTION('date', m.endDate) " +
           "ORDER BY m.endDate ASC")
    List<MaintenanceSchedule> findActiveMaintenance();
    
    /**
     * Find facilities that are under maintenance on a specific date from a list of facility IDs
     * 
     * @param facilityIds List of facility IDs to check
     * @param checkDate The date to check for maintenance
     * @return List of facility IDs that are under maintenance on the specified date
     */
    @Query("SELECT m.facilityId FROM MaintenanceSchedule m " +
       "WHERE m.facilityId IN :facilityIds " +
       "AND :checkDateString BETWEEN m.startDate AND m.endDate")
       List<Long> findFacilitiesUnderMaintenanceOnDate(@Param("facilityIds") List<Long> facilityIds, 
                                                 @Param("checkDateString") String checkDateString);

}