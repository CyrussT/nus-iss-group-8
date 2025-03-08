package com.group8.rbs.service.booking;

import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.dto.facility.FacilityResponseDTO;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.mapper.BookingFacilityMapper;
import com.group8.rbs.mapper.BookingMapper;
import com.group8.rbs.repository.BookingRepository;
import com.group8.rbs.repository.FacilityRepository;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final FacilityRepository facilityRepository;
    private final BookingFacilityMapper bookingFacilityMapper;

    public BookingService(BookingRepository bookingRepository, BookingMapper bookingMapper, FacilityRepository facilityRepository, BookingFacilityMapper bookingFacilityMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.facilityRepository = facilityRepository;
        this.bookingFacilityMapper = bookingFacilityMapper;
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
                            // Only include bookings for the specific date and with APPROVED or CONFIRMED status
                            LocalDate bookingDate = booking.getBookedDateTime().toLocalDate();
                            return bookingDate.equals(filterDate) && 
                                (booking.getStatus() == BookingStatus.APPROVED || 
                                booking.getStatus() == BookingStatus.CONFIRMED);
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
