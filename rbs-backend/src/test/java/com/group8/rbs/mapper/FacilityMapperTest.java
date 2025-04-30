package com.group8.rbs.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.group8.rbs.dto.facility.FacilityRequestDTO;
import com.group8.rbs.dto.facility.FacilityResponseDTO;
import com.group8.rbs.entities.Facility;

public class FacilityMapperTest {

    private FacilityMapper facilityMapper;
    private Facility facility;
    private FacilityRequestDTO facilityRequestDTO;

    @BeforeEach
    void setUp() {
        facilityMapper = new FacilityMapper();
        
        // Setup Facility
        facility = Facility.builder()
                .facilityId(1L)
                .resourceTypeId(2L)
                .resourceName("Test Facility")
                .location("Test Location")
                .capacity(50)
                .build();
        
        // Setup FacilityRequestDTO
        facilityRequestDTO = new FacilityRequestDTO();
        facilityRequestDTO.setResourceTypeId(2L);
        facilityRequestDTO.setResourceName("New Facility");
        facilityRequestDTO.setLocation("New Location");
        facilityRequestDTO.setCapacity(30);
    }

    @Test
    @DisplayName("Should map Facility to FacilityResponseDTO")
    void shouldMapFacilityToFacilityResponseDTO() {
        // Call the mapper
        FacilityResponseDTO response = facilityMapper.toDTO(facility);
        
        // Verify all fields are mapped correctly
        assertEquals(1L, response.getFacilityId());
        assertEquals(2L, response.getResourceTypeId());
        assertEquals("Test Facility", response.getResourceName());
        assertEquals("Test Location", response.getLocation());
        assertEquals(50, response.getCapacity());
    }
    
    @Test
    @DisplayName("Should map FacilityRequestDTO to Facility entity")
    void shouldMapFacilityRequestDTOToFacilityEntity() {
        // Call the mapper
        Facility result = facilityMapper.toEntity(facilityRequestDTO);
        
        // Verify fields are mapped correctly
        assertNull(result.getFacilityId()); // ID should not be set from request
        assertEquals(2L, result.getResourceTypeId());
        assertEquals("New Facility", result.getResourceName());
        assertEquals("New Location", result.getLocation());
        assertEquals(30, result.getCapacity());
        assertNull(result.getBookings()); // Bookings should not be set
    }
    
    @Test
    @DisplayName("Should handle null fields in Facility")
    void shouldHandleNullFieldsInFacility() {
        // Create facility with null fields
        Facility incompleteEntity = Facility.builder()
                .facilityId(1L)
                .resourceTypeId(null) // Null resource type
                .resourceName(null)   // Null resource name
                .location(null)       // Null location
                .capacity(null)       // Null capacity
                .build();
        
        // Call the mapper
        FacilityResponseDTO response = facilityMapper.toDTO(incompleteEntity);
        
        // Verify fields are still mapped, with nulls preserved
        assertEquals(1L, response.getFacilityId());
        assertNull(response.getResourceTypeId());
        assertNull(response.getResourceName());
        assertNull(response.getLocation());
        assertNull(response.getCapacity());
    }
    
    @Test
    @DisplayName("Should handle null fields in FacilityRequestDTO")
    void shouldHandleNullFieldsInFacilityRequestDTO() {
        // Create request DTO with null fields
        FacilityRequestDTO incompleteRequest = new FacilityRequestDTO();
        incompleteRequest.setResourceTypeId(null);
        incompleteRequest.setResourceName(null);
        incompleteRequest.setLocation(null);
        incompleteRequest.setCapacity(null);
        
        // Call the mapper
        Facility result = facilityMapper.toEntity(incompleteRequest);
        
        // Verify fields are still mapped, with nulls preserved
        assertNull(result.getResourceTypeId());
        assertNull(result.getResourceName());
        assertNull(result.getLocation());
        assertNull(result.getCapacity());
    }
    
    @Test
    @DisplayName("Should map message field in ResponseDTO")
    void shouldMapMessageFieldInResponseDTO() {
        // Call the mapper
        FacilityResponseDTO response = facilityMapper.toDTO(facility);
        
        // Initially, message should be null
        assertNull(response.getMessage());
        
        // Set a message
        response.setMessage("Facility created successfully!");
        
        // Verify message is set
        assertEquals("Facility created successfully!", response.getMessage());
    }
}