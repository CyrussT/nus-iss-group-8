package com.group8.rbs.repository;

import com.group8.rbs.entities.Facility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FacilityRepositoryTest {

    @Autowired
    private FacilityRepository facilityRepository;

    @Test
    void testSaveAndFindByResourceName() {
        Facility facility = new Facility();
        facility.setResourceName("Library");
        facility.setLocation("Block A");
        facility.setCapacity(100);
        facility.setResourceTypeId(1L);

        facilityRepository.save(facility);

        Optional<Facility> found = facilityRepository.findByResourceName("Library");

        assertThat(found).isPresent();
        assertThat(found.get().getLocation()).isEqualTo("Block A");
    }

    @Test
    void testFindByResourceNameAndLocation() {
        Facility facility = new Facility();
        facility.setResourceName("Lecture Hall");
        facility.setLocation("Building B");
        facility.setCapacity(200);
        facility.setResourceTypeId(2L);

        facilityRepository.save(facility);

        Optional<Facility> found = facilityRepository.findByResourceNameAndLocation("Lecture Hall", "Building B");

        assertThat(found).isPresent();
        assertThat(found.get().getCapacity()).isEqualTo(200);
    }

    @Test
    void testFindAllPageable() {
        Facility facility1 = new Facility();
        facility1.setResourceName("Auditorium");
        facility1.setLocation("Main Campus");
        facility1.setCapacity(300);
        facility1.setResourceTypeId(3L);

        Facility facility2 = new Facility();
        facility2.setResourceName("Gym");
        facility2.setLocation("Sports Complex");
        facility2.setCapacity(150);
        facility2.setResourceTypeId(4L);

        facilityRepository.save(facility1);
        facilityRepository.save(facility2);

        Page<Facility> page = facilityRepository.findAll(PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testFindAllResourceTypeIDs() {
        Facility facility = new Facility();
        facility.setResourceName("Computer Lab");
        facility.setLocation("IT Block");
        facility.setCapacity(40);
        facility.setResourceTypeId(5L);

        facilityRepository.save(facility);

        List<Long> resourceTypeIds = facilityRepository.findAllResourceTypeIDs();
        assertThat(resourceTypeIds).contains(5L);
    }

    @Test
    void testFindAllLocations() {
        Facility facility = new Facility();
        facility.setResourceName("Cafe");
        facility.setLocation("Student Center");
        facility.setCapacity(50);
        facility.setResourceTypeId(6L);

        facilityRepository.save(facility);

        List<String> locations = facilityRepository.findAllLocations();
        assertThat(locations).contains("Student Center");
    }

    @Test
    void testFindAllResourceNames() {
        Facility facility = new Facility();
        facility.setResourceName("Basketball Court");
        facility.setLocation("Sports Block");
        facility.setCapacity(100);
        facility.setResourceTypeId(7L);

        facilityRepository.save(facility);

        List<String> resourceNames = facilityRepository.findAllResourceNames();
        assertThat(resourceNames).contains("Basketball Court");
    }
}
