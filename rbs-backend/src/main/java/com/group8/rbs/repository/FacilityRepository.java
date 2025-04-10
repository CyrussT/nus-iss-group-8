package com.group8.rbs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.group8.rbs.entities.Facility;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long>,JpaSpecificationExecutor<Facility>  {
    Optional<Facility> findByResourceName(String resourceName);
    Optional<Facility> findByResourceNameAndLocation(String resourceName, String location);
    Page<Facility> findAll(Pageable pageable);

    @Query(value = "SELECT * FROM TBL_FACILITY f WHERE " +
    "(:resourceType IS NULL OR f.RESOURCE_TYPE = :resourceType) " +
    "AND (:resourceName IS NULL OR f.RESOURCE_NAME = :resourceName) " +
    "AND (:location IS NULL OR f.LOCATION = :location) " +
    "AND (:capacity IS NULL OR f.CAPACITY = :capacity) " +
    "ORDER BY f.RESOURCE_NAME", nativeQuery = true)
    List<Facility> searchFacilities(@Param("resourceType") String resourceType,
                                @Param("resourceName") String resourceName,
                                @Param("location") String location,
                                @Param("capacity") Integer capacity);
    
    @Query("SELECT DISTINCT f.resourceType FROM Facility f ORDER BY f.resourceType")
    List<String> findAllResourceTypes();
    
    @Query("SELECT DISTINCT f.location FROM Facility f ORDER BY f.location")
    List<String> findAllLocations();
    
    @Query("SELECT DISTINCT f.resourceName FROM Facility f ORDER BY f.resourceName")
    List<String> findAllResourceNames();
}
