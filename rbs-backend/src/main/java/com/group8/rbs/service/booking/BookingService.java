package com.group8.rbs.service.booking;

import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.dto.facility.FacilityNameOptionsResponse;
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
import com.group8.rbs.repository.FacilityTypeRepository;
import com.group8.rbs.validation.BookingValidationChainBuilder;
import com.group8.rbs.validation.BookingValidationContext;
import com.group8.rbs.validation.BookingValidator;
import com.group8.rbs.validation.ValidationResult;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final FacilityRepository facilityRepository;
    private final BookingFacilityMapper bookingFacilityMapper;
    private final AccountRepository accountRepository;
    private final FacilityTypeRepository facilityTypeRepository;
    private final CreditRepository creditRepository;
    private final BookingValidator validationChain;

    public BookingService(
        BookingRepository bookingRepository, 
        BookingMapper bookingMapper, 
        FacilityTypeRepository facilityTypeRepository,
        FacilityRepository facilityRepository, 
        BookingFacilityMapper bookingFacilityMapper,
        AccountRepository accountRepository,
        CreditRepository creditRepository,
        BookingValidationChainBuilder validationChainBuilder) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.facilityTypeRepository = facilityTypeRepository;
        this.facilityRepository = facilityRepository;
        this.bookingFacilityMapper = bookingFacilityMapper;
        this.accountRepository = accountRepository;
        this.creditRepository = creditRepository;
        this.validationChain = validationChainBuilder.buildValidationChain();
    }

    public List<FacilitySearchDTO> searchFacilities(FacilitySearchDTO searchCriteria) {
        Long resourceTypeId = searchCriteria.getResourceTypeId() != null 
            ? searchCriteria.getResourceTypeId() 
            : null;
                
        // Filter the facilities based on search criteria
        List<Facility> filteredFacilities = facilityRepository.searchFacilities(
            resourceTypeId,
            searchCriteria.getResourceName(),
            searchCriteria.getLocation(),
            searchCriteria.getCapacity());

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

    public Map<String, Object> getDropdownOptions() {
        Map<String, Object> options = new HashMap<>();

        // Convert Lists to List<Object> for the Map
        options.put("resourceTypes", getResourceTypes());
        options.put("locations", getLocations());
        options.put("resourceNames", getResourceNames());

        return options;
    }

    public List<FacilityNameOptionsResponse> getResourceTypes() {
        List<Object[]> results = facilityTypeRepository.findAllFacilityTypeOptions();
        
        return results.stream()
            .map(result -> {
                Long id = (Long) result[0];
                String name = (String) result[1];
                return new FacilityNameOptionsResponse(id, name);
            })
            .collect(Collectors.toList());
    }

    public List<String> getLocations() {
        return facilityRepository.findAllLocations();
    }

    public List<String> getResourceNames() {
        return facilityRepository.findAllResourceNames();
    }

    public BookingResponseDTO createBooking(BookingDTO requestDTO) {
        try {
            logger.info("Creating booking for facility ID: {}, date/time: {}", 
                    requestDTO.getFacilityId(), requestDTO.getBookedDateTime());
            
            // Find the account
            Optional<Account> accountOpt = accountRepository.findByEmail(requestDTO.getAccountEmail());
            if (accountOpt.isEmpty()) {
                throw new RuntimeException("Account not found");
            }
            Account account = accountOpt.get();
            
            // Run the validation chain
            ValidationResult result = validationChain.validate(requestDTO, account);
            if (!result.isValid()) {
                logger.warn("Booking validation failed: {}", result.getErrorMessage());
                throw new RuntimeException(result.getErrorMessage());
            }
            
            // Get the facility from validation context
            Facility facility = BookingValidationContext.getFacility();
            
            // Determine booking status based on facility type
            BookingStatus bookingStatus = facility.getResourceTypeId().equals(5L)
                    ? BookingStatus.PENDING // Sports & Recreation requires approval
                    : BookingStatus.APPROVED;
            
            // Deduct credits (moved after validation passes)
            Double creditsNeeded = Double.parseDouble(requestDTO.getCreditsUsed());
            int updatedRows = creditRepository.checkAndDeductCredits(account.getAccountId(), creditsNeeded);
            
            if (updatedRows == 0) {
                // This should never happen if validation passed, but as a safeguard
                Double currentBalance = creditRepository.findCreditBalanceByAccountId(account.getAccountId());
                throw new RuntimeException("Insufficient credits. Required: " + creditsNeeded + 
                                         ", Available: " + currentBalance);
            }
            
            // Create the booking entity
            Booking booking = Booking.builder()
                    .facility(facility)
                    .account(account)
                    .bookedDateTime(requestDTO.getBookedDateTime())
                    .timeSlot(requestDTO.getTimeSlot())
                    .title(requestDTO.getTitle())
                    .description(requestDTO.getDescription())
                    .status(bookingStatus)
                    .build();
            
            // Save to database
            Booking savedBooking = bookingRepository.save(booking);
            logger.info("Booking created successfully with ID: {}", savedBooking.getBookingId());
            
            // Return the response DTO
            return bookingMapper.toResponseDTO(savedBooking);
        } catch (Exception e) {
            logger.error("Error creating booking: {}", e.getMessage());
            throw e;
        } finally {
            // Always clean up context
            BookingValidationContext.clear();
        }
    }

    // Fetch upcoming approved bookings
    public List<BookingResponseDTO> getUpcomingApprovedOrConfirmedBookings(Long accountId) {
        LocalDateTime now = LocalDateTime.now(); // ✅ Get current date-time

        // ✅ Fetch only upcoming approved or confirmed bookings
        List<Booking> bookings = bookingRepository.findUpcomingApprovedOrConfirmedBookings(
                accountId, List.of(BookingStatus.APPROVED, BookingStatus.CONFIRMED), now);

        logger.info("Found " + bookings.size() + " upcoming approved/confirmed bookings");

        return bookings.stream()
                .map(bookingMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Fetch pending bookings with a future date
    public List<BookingResponseDTO> getPendingFutureBookings(Long accountId) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findByAccount_AccountIdAndStatusAndBookedDateTimeAfter(
                accountId, BookingStatus.PENDING, now);

        logger.info("Found " + bookings.size() + " pending bookings");
        return bookings.stream()
                .map(bookingMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getBookingHistory(Long accountId, String status) {
        LocalDateTime now = LocalDateTime.now();
        logger.info("Fetching booking history for studentId: " + accountId);
        logger.info("Current Server Time: " + now);

        List<Booking> bookings;

        if (status != null && !status.isEmpty()) {
            BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
            logger.info("Filtering by status: " + bookingStatus);
            bookings = bookingRepository.findByAccount_AccountIdAndStatusAndBookedDateTimeBefore(
                    accountId, bookingStatus, now);
        } else {
            logger.info("Fetching all past bookings (no status filter)");
            bookings = bookingRepository.findByAccount_AccountIdAndBookedDateTimeBefore(accountId, now);
        }

        for (Booking booking : bookings) {
            LocalDateTime bookedTime = booking.getBookedDateTime();
            logger.info("Booking ID: " + booking.getBookingId());
            logger.info("Booked DateTime: " + bookedTime);
            logger.info("Formatted DateTime: " + bookedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            logger.info("----------");
        }

        logger.info("Found " + bookings.size() + " past bookings");
        
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