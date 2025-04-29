package com.group8.rbs.service.facility;

import com.group8.rbs.dto.facility.FacilityRequestDTO;
import com.group8.rbs.dto.facility.FacilityResponseDTO;
import com.group8.rbs.dto.facility.FacilitySearchDTO;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.exception.FacilityException;
import com.group8.rbs.mapper.FacilityMapper;
import com.group8.rbs.repository.FacilityRepository;
import com.group8.rbs.service.facility.FacilityService;
import com.group8.rbs.repository.BookingRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FacilityServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private FacilityMapper facilityMapper;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private FacilityService facilityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateFacility_success() {
        FacilityRequestDTO request = new FacilityRequestDTO();
        request.setResourceName("Library");
        request.setLocation("Campus A");

        Facility facility = new Facility();
        Facility savedFacility = new Facility();
        savedFacility.setFacilityId(1L);

        FacilityResponseDTO responseDTO = new FacilityResponseDTO();
        responseDTO.setFacilityId(1L);
        responseDTO.setMessage("Facility created successfully!");

        when(facilityRepository.findByResourceNameAndLocation("Library", "Campus A"))
                .thenReturn(Optional.empty());
        when(facilityMapper.toEntity(any())).thenReturn(facility);
        when(facilityRepository.save(facility)).thenReturn(savedFacility);
        when(facilityMapper.toDTO(savedFacility)).thenReturn(responseDTO);

        FacilityResponseDTO result = facilityService.createFacility(request);

        assertEquals(1L, result.getFacilityId());
        assertEquals("Facility created successfully!", result.getMessage());
    }

    @Test
    void testCreateFacility_alreadyExists() {
        FacilityRequestDTO request = new FacilityRequestDTO();
        request.setResourceName("Library");
        request.setLocation("Campus A");

        when(facilityRepository.findByResourceNameAndLocation(anyString(), anyString()))
                .thenReturn(Optional.of(new Facility()));

        assertThrows(FacilityException.FacilityAlreadyExistsException.class, () ->
                facilityService.createFacility(request));
    }

    @Test
    void testGetAllFacilities() {
        List<Facility> facilities = Collections.singletonList(new Facility());
        Page<Facility> facilityPage = new PageImpl<>(facilities);

        when(facilityRepository.findAll(any(Pageable.class))).thenReturn(facilityPage);
        when(facilityMapper.toDTO(any(Facility.class))).thenReturn(new FacilityResponseDTO());

        Page<FacilityResponseDTO> result = facilityService.getAllFacilities(0, 10);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetFacilityById_found() {
        Facility facility = new Facility();
        facility.setFacilityId(1L);
        FacilityResponseDTO dto = new FacilityResponseDTO();
        dto.setFacilityId(1L);

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(facilityMapper.toDTO(facility)).thenReturn(dto);

        FacilityResponseDTO result = facilityService.getFacilityById(1L);
        assertEquals(1L, result.getFacilityId());
    }

    @Test
    void testGetFacilityById_notFound() {
        when(facilityRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(FacilityException.FacilityNotFoundException.class, () -> facilityService.getFacilityById(1L));
    }

    @Test
    void testUpdateFacility_found() {
        // Arrange
        FacilityRequestDTO request = new FacilityRequestDTO();
        request.setResourceName("Updated");
        request.setLocation("Updated Location");
        request.setCapacity(100);
    
        Facility existingFacility = new Facility();
        existingFacility.setFacilityId(1L);
        existingFacility.setResourceName("Old Name"); // simulate old name
        existingFacility.setLocation("Old Location"); // simulate old location
    
        Facility updatedFacility = new Facility();
        updatedFacility.setFacilityId(1L);
    
        FacilityResponseDTO responseDTO = new FacilityResponseDTO();
        responseDTO.setFacilityId(1L);
        responseDTO.setMessage("Facility updated successfully!");
    
        // Mock repository and mapper behavior
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(existingFacility));
        when(facilityRepository.findByResourceNameAndLocation("Updated", "Updated Location")).thenReturn(Optional.empty()); // No conflict
        when(facilityRepository.save(any(Facility.class))).thenReturn(updatedFacility);
        when(facilityMapper.toDTO(updatedFacility)).thenReturn(responseDTO);
    
        // Act
        FacilityResponseDTO result = facilityService.updateFacility(1L, request);
    
        // Assert
        assertEquals(1L, result.getFacilityId());
        assertEquals("Facility updated successfully!", result.getMessage());
    }
    

    @Test
    void testDeleteFacility_found() {
        Facility facility = new Facility();
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        doNothing().when(facilityRepository).delete(facility);

        assertDoesNotThrow(() -> facilityService.deleteFacility(1L));
    }

    @Test
    void testDeleteFacility_notFound() {
        when(facilityRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(FacilityException.FacilityNotFoundException.class, () -> facilityService.deleteFacility(1L));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSearchFacilities() {
        FacilitySearchDTO searchDTO = new FacilitySearchDTO();
        Page<Facility> page = new PageImpl<>(List.of(new Facility()));
        when(facilityRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(facilityMapper.toDTO(any())).thenReturn(new FacilityResponseDTO());

        Page<FacilityResponseDTO> result = facilityService.searchFacilities(searchDTO, 0, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testGetFacilityWithBookings() {
        Facility facility = new Facility();
        facility.setFacilityId(1L);
        facility.setResourceName("Lab");
        facility.setLocation("Campus A");
        facility.setCapacity(50);

        Booking booking = new Booking();
        booking.setFacility(facility);

        Account account = new Account();
        account.setAccountId(100L);
        account.setName("Student");
        account.setEmail("student@example.com");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAccount(account);

        List<Booking> bookings = List.of(booking);

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(bookingRepository.findByFacility_FacilityId(1L)).thenReturn(bookings);

        FacilityResponseDTO result = facilityService.getFacilityWithBookings(1L);

        assertEquals(1L, result.getFacilityId());
        assertEquals("Lab", result.getResourceName());
        assertEquals(1, result.getBookings().size());
        assertEquals("Student", result.getBookings().get(0).getStudentName());
        assertEquals("CONFIRMED", result.getBookings().get(0).getStatus()); 
    }
}
