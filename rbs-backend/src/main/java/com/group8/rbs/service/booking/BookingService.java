package com.group8.rbs.service.booking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.group8.rbs.entities.Facility;
import com.group8.rbs.repository.BookingRepository;
import com.group8.rbs.repository.FacilityRepository;
import com.group8.rbs.dto.booking.FacilitySearchDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final FacilityRepository facilityRepository;

    public List<Facility> getFacilities() {
        return facilityRepository.findAll();
    }
    
    public List<Facility> searchFacilities(FacilitySearchDTO searchCriteria) {
        List<Facility> allFacilities = facilityRepository.findAll();
        
        // Filter the facilities based on search criteria
        return allFacilities.stream()
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
    }
    
    public Map<String, List<Object>> getDropdownOptions() {
        Map<String, List<Object>> options = new HashMap<>();
        
        // Convert Lists to List<Object> for the Map
        options.put("resourceTypes", getResourceTypes().stream().map(t -> (Object) t).collect(Collectors.toList()));
        options.put("locations", getLocations().stream().map(l -> (Object) l).collect(Collectors.toList()));
        options.put("resourceNames", getResourceNames().stream().map(n -> (Object) n).collect(Collectors.toList()));
        
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
}