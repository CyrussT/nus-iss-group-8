package com.group8.rbs.mapper;

import org.springframework.stereotype.Component;

import com.group8.rbs.dto.facility.FacilityRequestDTO;
import com.group8.rbs.dto.facility.FacilityResponseDTO;
import com.group8.rbs.entities.Facility;

@Component
public class FacilityMapper {
    public FacilityResponseDTO toDTO(Facility facility) {
        FacilityResponseDTO dto = new FacilityResponseDTO();
        dto.setFacilityId(facility.getFacilityId());
        dto.setResourceType(facility.getResourceType());
        dto.setResourceName(facility.getResourceName());
        dto.setLocation(facility.getLocation());
        dto.setCapacity(facility.getCapacity());
        return dto;
    }

    public Facility toEntity(FacilityRequestDTO dto) {
        return Facility.builder()
                .resourceType(dto.getResourceType())
                .resourceName(dto.getResourceName())
                .location(dto.getLocation())
                .capacity(dto.getCapacity())
                .build();
    }
}
