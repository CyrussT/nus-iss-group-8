package com.group8.rbs.specification;

import org.springframework.data.jpa.domain.Specification;

import com.group8.rbs.dto.facility.FacilitySearchDTO;
import com.group8.rbs.entities.Facility;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class FacilitySpecification {
    
    public static Specification<Facility> searchFacilities(FacilitySearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDTO.getResourceType() != null && !searchDTO.getResourceType().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("resourceType"), searchDTO.getResourceType()));
            }
            if (searchDTO.getResourceName() != null && !searchDTO.getResourceName().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("resourceName"), searchDTO.getResourceName()));
            }
            if (searchDTO.getLocation() != null && !searchDTO.getLocation().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("location"), searchDTO.getLocation()));
            }
            if (searchDTO.getCapacity() != null) {
                predicates.add(criteriaBuilder.equal(root.get("capacity"), searchDTO.getCapacity()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
