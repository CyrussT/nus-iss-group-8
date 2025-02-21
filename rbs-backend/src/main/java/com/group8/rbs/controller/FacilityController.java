package com.group8.rbs.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    
    @GetMapping
    public ResponseEntity<List<FacilityResponseDTO>> getAllFacilities() {
        return ResponseEntity.ok(facilityService.getAllFacilities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacilityResponseDTO> getFacilityById(@PathVariable Long id) {
        return ResponseEntity.ok(facilityService.getFacilityById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacilityResponseDTO> updateFacility(@PathVariable Long id, @RequestBody FacilityRequestDTO facilityRequestDTO) {
        FacilityResponseDTO facility = facilityService.updateFacility(id, facilityRequestDTO);
        return ResponseEntity.ok(facility);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFacility(@PathVariable Long id) {
        facilityService.deleteFacility(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public ResponseEntity<List<FacilityResponseDTO>> searchFacilities(@RequestBody FacilitySearchDTO searchDTO) {
        List<FacilityResponseDTO> facilities = facilityService.searchFacilities(searchDTO);
        return ResponseEntity.ok(facilities);
    }
}