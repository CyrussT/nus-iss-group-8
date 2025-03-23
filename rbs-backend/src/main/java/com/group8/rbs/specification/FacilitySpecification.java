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

            if (searchDTO.getResourceTypeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("resourceTypeID"), searchDTO.getResourceTypeId()));
            }
            

            if (searchDTO.getResourceName() != null && !searchDTO.getResourceName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("resourceName")),
                    "%" + searchDTO.getResourceName().toLowerCase().trim() + "%"
                ));
            }

            if (searchDTO.getLocation() != null && !searchDTO.getLocation().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("location")),
                    "%" + searchDTO.getLocation().toLowerCase().trim() + "%"
                ));
            }

            if (searchDTO.getCapacity() != null) {
                predicates.add(criteriaBuilder.equal(root.get("capacity"), searchDTO.getCapacity()));  // ✅ Capacity remains exact match
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}