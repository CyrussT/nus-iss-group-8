package com.group8.rbs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.group8.rbs.dto.facility.FacilityTypeDTO;
import com.group8.rbs.entities.FacilityType;
import com.group8.rbs.repository.FacilityTypeRepository;

import java.util.List;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/facility-types")
public class FacilityTypeController {

    @Autowired
    private FacilityTypeRepository facilityTypeRepository;

    // Endpoint to get distinct resource type names and their ids
    @GetMapping("/all")
    public ResponseEntity<List<FacilityTypeDTO>> getAllFacilityTypes() {
        List<FacilityTypeDTO> facilityTypes = facilityTypeRepository.findAll()
            .stream()
            .map(ft -> new FacilityTypeDTO(ft.getId(), ft.getName()))
            .collect(Collectors.toList());

    return ResponseEntity.ok(facilityTypes);
}

}    