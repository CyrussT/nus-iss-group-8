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

}
