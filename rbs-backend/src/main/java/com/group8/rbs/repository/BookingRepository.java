package com.group8.rbs.repository;

import com.group8.rbs.entities.Booking;
import com.group8.rbs.enums.BookingStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByAccount_AccountId(Long accountId);

    List<Booking> findByAccount_AccountIdAndStatus(Long accountId, BookingStatus status);

    // Past Bookings
    List<Booking> findByAccount_AccountIdAndBookedDateTimeBefore(Long accountId, LocalDateTime now);

    List<Booking> findByAccount_AccountIdAndStatusAndBookedDateTimeBefore(Long accountId, BookingStatus status,
            LocalDateTime now);

    // Pending Bookings Based on status and bookedDateTime
    List<Booking> findByAccount_AccountIdAndStatusAndBookedDateTimeAfter(Long accountId, BookingStatus status,
            LocalDateTime currentDateTime);

    List<Booking> findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(Long facilityId,
            LocalDateTime startDateTime, LocalDateTime endDateTime, List<BookingStatus> statuses);

    // Upcoming Approved or Approved Bookings
    @Query("SELECT b FROM Booking b WHERE b.account.accountId = :accountId AND b.status IN :statuses AND b.bookedDateTime > :now")
    List<Booking> findUpcomingApprovedOrConfirmedBookings(
            @Param("accountId") Long accountId,
            @Param("statuses") List<BookingStatus> statuses,
            @Param("now") LocalDateTime now);

    // Fetch bookings by Facility ID
    List<Booking> findByFacility_FacilityId(Long facilityId);

    List<Booking> findByStatus(BookingStatus status);
}
