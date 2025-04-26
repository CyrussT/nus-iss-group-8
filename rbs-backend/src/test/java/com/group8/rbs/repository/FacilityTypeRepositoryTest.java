package com.group8.rbs.repository;

import com.group8.rbs.entities.FacilityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FacilityTypeRepositoryTest {

    @Autowired
    private FacilityTypeRepository facilityTypeRepository;

    @Test
    void testFindDistinctNames() {
        // Setup test data
        FacilityType type1 = new FacilityType();
        type1.setName("Lecture Hall");

        FacilityType type2 = new FacilityType();
        type2.setName("Laboratory");

        FacilityType type3 = new FacilityType();
        type3.setName("Lecture Hall"); // duplicate on purpose

        facilityTypeRepository.save(type1);
        facilityTypeRepository.save(type2);
        facilityTypeRepository.save(type3);

        // Act
        List<String> distinctNames = facilityTypeRepository.findDistinctNames();

        // Assert
        assertThat(distinctNames).containsExactlyInAnyOrder("Lecture Hall", "Laboratory");
        assertThat(distinctNames.size()).isEqualTo(2); // Only 2 distinct names
    }

    @Test
    void testFindAllFacilityTypeOptions() {
        FacilityType type1 = new FacilityType();
        type1.setName("Cafeteria");

        FacilityType type2 = new FacilityType();
        type2.setName("Auditorium");

        facilityTypeRepository.save(type1);
        facilityTypeRepository.save(type2);

        List<Object[]> options = facilityTypeRepository.findAllFacilityTypeOptions();

        assertThat(options).isNotEmpty();
        assertThat(options.size()).isEqualTo(2);

        for (Object[] option : options) {
            assertThat(option[0]).isInstanceOf(Long.class);  // ID
            assertThat(option[1]).isInstanceOf(String.class); // Name
        }
    }
}
