package com.group8.rbs.service.booking;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.mapper.BookingFacilityMapper;
import com.group8.rbs.mapper.BookingMapper;
import com.group8.rbs.repository.AccountRepository;
import com.group8.rbs.repository.BookingRepository;
import com.group8.rbs.repository.CreditRepository;
import com.group8.rbs.repository.FacilityRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final FacilityRepository facilityRepository;
    private final BookingFacilityMapper bookingFacilityMapper;
    private final AccountRepository accountRepository;
    private final CreditRepository creditRepository;

    public BookingService(
        BookingRepository bookingRepository, 
        BookingMapper bookingMapper, 
        FacilityRepository facilityRepository, 
        BookingFacilityMapper bookingFacilityMapper,
        AccountRepository accountRepository,
        CreditRepository creditRepository
        ) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.facilityRepository = facilityRepository;
        this.bookingFacilityMapper = bookingFacilityMapper;
        this.accountRepository = accountRepository;
        this.creditRepository = creditRepository;
    }

    public List<FacilitySearchDTO> searchFacilities(FacilitySearchDTO searchCriteria) {
        // Filter the facilities based on search criteria
        List<Facility> filteredFacilities = facilityRepository.findAll().stream()
                .filter(facility -> {
                    // Resource Type filter
                    if (StringUtils.hasText(searchCriteria.getResourceType()) &&
                            !facility.getResourceType().toLowerCase().contains(
                                    searchCriteria.getResourceType().toLowerCase())) {
                        return false;
                    }

                    // Resource Name filter
                    if (StringUtils.hasText(searchCriteria.getResourceName()) &&
                            !facility.getResourceName().toLowerCase().contains(
                                    searchCriteria.getResourceName().toLowerCase())) {
                        return false;
                    }

                    // Location filter
                    if (StringUtils.hasText(searchCriteria.getLocation()) &&
                            !facility.getLocation().toLowerCase().contains(
                                    searchCriteria.getLocation().toLowerCase())) {
                        return false;
                    }

                    // Capacity filter
                    if (searchCriteria.getCapacity() != null &&
                            facility.getCapacity() < searchCriteria.getCapacity()) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // If date filter is provided, use it to filter bookings within each facility
        if (searchCriteria.getDate() != null) {
            LocalDate filterDate = searchCriteria.getDate();

            // For each facility, filter its bookings to only those on the specified date
            filteredFacilities.forEach(facility -> {
                if (facility.getBookings() != null && !facility.getBookings().isEmpty()) {
                    List<Booking> filteredBookings = facility.getBookings().stream()
                            .filter(booking -> {
                                // Only include bookings for the specific date and with APPROVED or CONFIRMED
                                // status
                                LocalDate bookingDate = booking.getBookedDateTime().toLocalDate();
                                return bookingDate.equals(filterDate) &&
                                        (booking.getStatus() == BookingStatus.APPROVED ||
                                                booking.getStatus() == BookingStatus.CONFIRMED ||
                                                booking.getStatus() == BookingStatus.PENDING);
                            })
                            .collect(Collectors.toList());

                    // Replace the bookings list with the filtered one
                    facility.setBookings(filteredBookings);
                }
            });
        }

        // Map to response DTOs with bookings included
        return filteredFacilities.stream()
                .map(bookingFacilityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public Map<String, List<String>> getDropdownOptions() {
        Map<String, List<String>> options = new HashMap<>();

        // Convert Lists to List<Object> for the Map
        options.put("resourceTypes", getResourceTypes());
        options.put("locations", getLocations());
        options.put("resourceNames", getResourceNames());

        return options;
    }

    public List<String> getResourceTypes() {
        return facilityRepository.findAllResourceTypes();
    }

    public List<String> getLocations() {
        return facilityRepository.findAllLocations();
    }

    public List<String> getResourceNames() {
        return facilityRepository.findAllResourceNames();
    }

    public BookingResponseDTO createBooking(BookingDTO requestDTO) {
      // Find the facility
      Facility facility = facilityRepository.findById(requestDTO.getFacilityId())
              .orElseThrow(() -> new RuntimeException("Facility not found"));

      // Find the account
      Optional<Account> account = accountRepository.findByEmail(requestDTO.getAccountEmail());

      if (account.isEmpty()) {
          throw new RuntimeException("Account not found");
      }

      // Check if the time slot is available
      if (!isTimeSlotAvailable(requestDTO.getFacilityId(), requestDTO.getBookedDateTime(), requestDTO.getTimeSlot())) {
          throw new RuntimeException("This time slot is already booked");
      }

      // Parse the credits needed from the request
      Double creditsNeeded;
      try {
          creditsNeeded = Double.parseDouble(requestDTO.getCreditsUsed());
      } catch (NumberFormatException e) {
          throw new RuntimeException("Invalid credits used value: " + requestDTO.getCreditsUsed(), e);
      }
      // Attempt to deduct credits - this will only succeed if sufficient credits exist
      int updatedRows = creditRepository.checkAndDeductCredits(account.get().getAccountId(), creditsNeeded);

      if (updatedRows == 0) {
          // Get current balance for a better error message
          Double currentBalance = creditRepository.findCreditBalanceByAccountId(account.get().getAccountId());
          throw new RuntimeException("Insufficient credits. Required: " + creditsNeeded + 
                                     ", Available: " + currentBalance);

      // To set to pending or instant approve based on facility type
      BookingStatus bookingStatus = facility.getResourceType().equals("5")
              ? BookingStatus.PENDING // Sports & Recreation requires approval
              : BookingStatus.APPROVED;

      // Create the booking entity
      Booking booking = Booking.builder()
              .facility(facility)
              .account(account.get())
              .bookedDateTime(requestDTO.getBookedDateTime())
              .timeSlot(requestDTO.getTimeSlot())
              .status(bookingStatus)
              .build();

      // Save to database
      Booking savedBooking = bookingRepository.save(booking);

      // Return the response DTO
      return bookingMapper.toResponseDTO(savedBooking);
    }

    // Helper method to check if a time slot is available
    private boolean isTimeSlotAvailable(Long facilityId, LocalDateTime bookedDateTime, String timeSlot) {
        // Get all bookings for this facility on this date with APPROVED or PENDING
        // status

        // Extract the date part from bookedDateTime
        LocalDate bookingDate = bookedDateTime.toLocalDate();

        // Create start and end of day for the date range query
        LocalDateTime startOfDay = bookingDate.atStartOfDay();
        LocalDateTime endOfDay = bookingDate.plusDays(1).atStartOfDay().minusNanos(1);

        // Query using time range
        List<Booking> existingBookings = bookingRepository.findByFacility_FacilityIdAndBookedDateTimeBetweenAndStatusIn(
                facilityId,
                startOfDay,
                endOfDay,
                Arrays.asList(BookingStatus.APPROVED, BookingStatus.CONFIRMED, BookingStatus.PENDING));

        // Check for overlap
        for (Booking booking : existingBookings) {
            if (booking.getTimeSlot().equals(timeSlot)) {
                return false; // Time slot already booked
            }
        }

        return true; // Time slot is available
    }

    // Fetch upcoming bookings that are APPROVED or CONFIRMED
    public List<BookingResponseDTO> getUpcomingApprovedOrConfirmedBookings(Long accountId) {
        LocalDateTime now = LocalDateTime.now(); // ✅ Get current date-time

        // ✅ Fetch only upcoming approved or confirmed bookings
        List<Booking> bookings = bookingRepository.findUpcomingApprovedOrConfirmedBookings(
                accountId, List.of(BookingStatus.APPROVED, BookingStatus.CONFIRMED), now);

        System.out.println("Found " + bookings.size() + " upcoming approved/confirmed bookings");

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

    public boolean deleteBooking(Long bookingId) {
        Optional<Booking> booking = bookingRepository.findById(bookingId);

        if (booking.isPresent()) {
            bookingRepository.delete(booking.get());
            return true;
        } else {
            return false;
        }
    }

    public List<BookingResponseDTO> getBookingsByStatus(String status) {

        List<Booking> bookings = bookingRepository.findByStatus(BookingStatus.valueOf(status));

        return bookings.stream()
                .map(bookingMapper::toResponseDTO)
                .toList();
    }

    public boolean updateBookingStatus(Long bookingId, BookingStatus status) {
        Optional<Booking> optionalBooking = bookingRepository.findById(bookingId);

        if (optionalBooking.isPresent()) {
            Booking booking = optionalBooking.get();
            booking.setStatus(status);
            bookingRepository.save(booking);
            return true; // Return true if update was successful
        } else {
            return false; // Return false if booking not found
        }
    }
}
