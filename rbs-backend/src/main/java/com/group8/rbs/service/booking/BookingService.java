package com.group8.rbs.service.booking;

import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.mapper.BookingMapper;
import com.group8.rbs.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    public BookingService(BookingRepository bookingRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
    }

    // Fetch upcoming approved bookings
    public List<BookingResponseDTO> getUpcomingApprovedBookings(Long accountId) {
        LocalDateTime now = LocalDateTime.now(); // ✅ Get current date-time
    
        List<Booking> bookings = bookingRepository.findByAccount_AccountIdAndStatusAndBookedDateTimeAfter(
                accountId, BookingStatus.APPROVED, now); // ✅ Fetch only upcoming approved bookings
    
        System.out.println("Found " + bookings.size() + " upcoming approved bookings");
        return bookings.stream()
                .map(bookingMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
    

    // Fetch pending bookings with a future date
    public List<BookingResponseDTO> getPendingFutureBookings(Long accountId) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findByAccount_AccountIdAndStatusAndBookedDateTimeAfter(
                accountId, BookingStatus.PENDING, now);

        System.out.println("Found " + bookings.size() + " pending bookings");
        return bookings.stream()
                .map(bookingMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getBookingHistory(Long accountId, String status) {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("Fetching booking history for studentId: " + accountId);

        List<Booking> bookings;

        if (status != null && !status.isEmpty()) {
            BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
            System.out.println("Filtering by status: " + bookingStatus);
            bookings = bookingRepository.findByAccount_AccountIdAndStatusAndBookedDateTimeBefore(
                    accountId, bookingStatus, now);
        } else {
            System.out.println("Fetching all past bookings (no status filter)");
            bookings = bookingRepository.findByAccount_AccountIdAndBookedDateTimeBefore(accountId, now);
        }
    
        System.out.println("Found " + bookings.size() + " past bookings");
        return bookings.stream().map(bookingMapper::toResponseDTO).collect(Collectors.toList());
    }

    
    


}
