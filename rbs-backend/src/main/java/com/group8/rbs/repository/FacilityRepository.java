package com.group8.rbs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.group8.rbs.entities.Facility;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    @Query("SELECT DISTINCT f.resourceType FROM Facility f ORDER BY f.resourceType")
    List<String> findAllResourceTypes();
    
    @Query("SELECT DISTINCT f.location FROM Facility f ORDER BY f.location")
    List<String> findAllLocations();
    
    @Query("SELECT DISTINCT f.resourceName FROM Facility f ORDER BY f.resourceName")
    List<String> findAllResourceNames();
    
}
