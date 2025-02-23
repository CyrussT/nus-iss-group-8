package com.group8.rbs.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.group8.rbs.dto.facility.FacilityRequestDTO;
import com.group8.rbs.dto.facility.FacilityResponseDTO;
import com.group8.rbs.dto.facility.FacilitySearchDTO;
import com.group8.rbs.service.facility.FacilityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {
    private final FacilityService facilityService;
    private static final Logger logger = LoggerFactory.getLogger(FacilityService.class);

    @Autowired
    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @PostMapping("/create")
    public ResponseEntity<FacilityResponseDTO> createFacility(@Valid @RequestBody FacilityRequestDTO facilityRequestDTO) {
        FacilityResponseDTO newFacility = facilityService.createFacility(facilityRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(newFacility);
    }
    
    @GetMapping("/list")
    public ResponseEntity<Page<FacilityResponseDTO>> getAllFacilities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<FacilityResponseDTO> facilities = facilityService.getAllFacilities(page, size);
        return ResponseEntity.ok(facilities);
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<FacilityResponseDTO> getFacilityById(@PathVariable Long id) {
        return ResponseEntity.ok(facilityService.getFacilityById(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<FacilityResponseDTO> updateFacility(@PathVariable Long id, @RequestBody FacilityRequestDTO facilityRequestDTO) {
        FacilityResponseDTO facility = facilityService.updateFacility(id, facilityRequestDTO);
        return ResponseEntity.ok(facility);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteFacility(@PathVariable Long id) {
        facilityService.deleteFacility(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<FacilityResponseDTO>> searchFacilities(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceName,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        FacilitySearchDTO searchDTO = new FacilitySearchDTO();
        searchDTO.setResourceType(resourceType);
        searchDTO.setResourceName(resourceName);
        searchDTO.setLocation(location);
        searchDTO.setCapacity(capacity);

        Page<FacilityResponseDTO> facilities = facilityService.searchFacilities(searchDTO, page, size);
        return ResponseEntity.ok(facilities);
    }
}