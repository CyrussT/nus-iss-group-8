package com.group8.rbs.service.facility;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.group8.rbs.dto.facility.FacilityRequestDTO;
import com.group8.rbs.dto.facility.FacilityResponseDTO;
import com.group8.rbs.dto.facility.FacilitySearchDTO;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.exception.FacilityException;
import com.group8.rbs.mapper.FacilityMapper;
import com.group8.rbs.repository.FacilityRepository;
import com.group8.rbs.specification.FacilitySpecification;

import jakarta.validation.Valid;


@Service
public class FacilityService {
    private final FacilityRepository facilityRepository;
    private final FacilityMapper facilityMapper;
    private static final Logger logger = LoggerFactory.getLogger(FacilityService.class);

    @Autowired
    public FacilityService(FacilityRepository facilityRepository, FacilityMapper facilityMapper) {
        this.facilityRepository = facilityRepository;
        this.facilityMapper = facilityMapper;
    }

    // Create facility
    public FacilityResponseDTO createFacility(FacilityRequestDTO facilityRequestDTO) {
        Optional<Facility> existingFacility = facilityRepository.findByResourceNameAndLocation(
            facilityRequestDTO.getResourceName(), facilityRequestDTO.getLocation()
        );

        if (existingFacility.isPresent()) {
            throw new FacilityException.FacilityAlreadyExistsException("Facility with this name and location already exists.");
        }

        Facility facility = facilityMapper.toEntity(facilityRequestDTO);
        Facility savedFacility = facilityRepository.save(facility);

        FacilityResponseDTO responseDTO = facilityMapper.toDTO(savedFacility);
        responseDTO.setMessage("Facility created successfully!");
        return responseDTO;
    }

    public List<FacilityResponseDTO> getAllFacilities() {
        return facilityRepository.findAll().stream()
                .map(facilityMapper::toDTO)
                .collect(Collectors.toList());
    }


    public FacilityResponseDTO getFacilityById(Long id) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new FacilityException.FacilityNotFoundException("Facility not found"));
        return facilityMapper.toDTO(facility);
    }


    public FacilityResponseDTO updateFacility(Long id, FacilityRequestDTO facilityRequestDTO) {
        return facilityRepository.findById(id).map(facility -> {
            facility.setResourceType(facilityRequestDTO.getResourceType());
            facility.setResourceName(facilityRequestDTO.getResourceName());
            facility.setLocation(facilityRequestDTO.getLocation());
            facility.setCapacity(facilityRequestDTO.getCapacity());
            Facility updatedFacility = facilityRepository.save(facility);
            return facilityMapper.toDTO(updatedFacility);
        }).orElseThrow(() -> new RuntimeException("Facility not found"));
    }


    public void deleteFacility(Long id) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new FacilityException.FacilityNotFoundException("Facility not found"));
        facilityRepository.delete(facility);
    }

    public Page<FacilityResponseDTO> searchFacilities(FacilitySearchDTO searchDTO, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Facility> facilityPage = facilityRepository.findAll(FacilitySpecification.searchFacilities(searchDTO), pageable);

        return facilityPage.map(facilityMapper::toDTO);
    }

    
}
