package com.group8.rbs.repository;

import java.util.Optional;

import com.group8.rbs.entities.Booking;
import com.group8.rbs.enums.BookingStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByAccount_AccountId(Long accountId);
    List<Booking> findByAccount_AccountIdAndStatus(Long accountId, BookingStatus status);

    //  Past Bookings
    List<Booking> findByAccount_AccountIdAndBookedDateTimeBefore(Long accountId, LocalDateTime now);
    List<Booking> findByAccount_AccountIdAndStatusAndBookedDateTimeBefore(Long accountId, BookingStatus status, LocalDateTime now);

    // Pending & Upcoming Bookings Based on status and bookedDateTime
    List<Booking> findByAccount_AccountIdAndStatusAndBookedDateTimeAfter(Long accountId, BookingStatus status, LocalDateTime currentDateTime);



}
