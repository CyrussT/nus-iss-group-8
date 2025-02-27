package com.group8.rbs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.service.booking.BookingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    
    @GetMapping("/facilities")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<List<Facility>> getFacilities() {
        return ResponseEntity.ok(bookingService.getFacilities());
    }

     @GetMapping("/facilities/search")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<List<Facility>> searchFacilities(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceName,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer capacity) {
        
        FacilitySearchDTO searchCriteria = FacilitySearchDTO.builder()
                .resourceType(resourceType)
                .resourceName(resourceName)
                .location(location)
                .capacity(capacity)
                .build();
                
        return ResponseEntity.ok(bookingService.searchFacilities(searchCriteria));
    }

    @GetMapping("/dropdown-options")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<Map<String, List<Object>>> getDropdownOptions() {
        Map<String, List<Object>> options = bookingService.getDropdownOptions();
        return ResponseEntity.ok(options);
    }
    
    @GetMapping("/resource-types")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<List<Map<String, String>>> getResourceTypes() {
        List<String> types = bookingService.getResourceTypes();
        List<Map<String, String>> formattedTypes = types.stream()
            .map(type -> Map.of("label", type, "value", type))
            .collect(Collectors.toList());
        return ResponseEntity.ok(formattedTypes);
    }
    
    @GetMapping("/locations")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<List<Map<String, String>>> getLocations() {
        List<String> locations = bookingService.getLocations();
        List<Map<String, String>> formattedLocations = locations.stream()
            .map(location -> Map.of("label", location, "value", location))
            .collect(Collectors.toList());
        return ResponseEntity.ok(formattedLocations);
    }
    
    @GetMapping("/resource-names")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<List<Map<String, String>>> getResourceNames() {
        List<String> names = bookingService.getResourceNames();
        List<Map<String, String>> formattedNames = names.stream()
            .map(name -> Map.of("label", name, "value", name))
            .collect(Collectors.toList());
        return ResponseEntity.ok(formattedNames);
    }
}
