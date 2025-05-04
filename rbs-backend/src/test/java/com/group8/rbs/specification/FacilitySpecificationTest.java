package com.group8.rbs.specification;


import com.group8.rbs.dto.facility.FacilitySearchDTO;
import com.group8.rbs.entities.Facility;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
class FacilitySpecificationTest {

    @Test
    void testSearchFacilities_withAllFields() {
        // Prepare DTO
        FacilitySearchDTO dto = new FacilitySearchDTO();
        dto.setResourceTypeId(1L);
        dto.setResourceName("Projector");
        dto.setLocation("East Wing");
        dto.setCapacity(20);

        // Create spec
        Specification<Facility> spec = FacilitySpecification.searchFacilities(dto);

        // Mock JPA Criteria objects
        Root<Facility> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        // Mock predicate behavior
        Path<Object> path = mock(Path.class);
        when(root.get(anyString())).thenReturn(path);
        when(cb.lower(any())).thenReturn(mock(Expression.class));
        when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
        when(cb.equal(any(), any())).thenReturn(mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        // Execute
        Predicate result = spec.toPredicate(root, query, cb);

        // Assert
        assertNotNull(result);
    }
}