package com.group8.rbs.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.group8.rbs.entities.FacilityType;

import java.util.List;

@Repository
public interface FacilityTypeRepository extends JpaRepository<FacilityType, Long> {

    // Custom query to fetch distinct names
    @Query("SELECT DISTINCT ft.name FROM FacilityType ft")
    List<String> findDistinctNames();
    // query to fetch dropdown options
    @Query("SELECT DISTINCT ft.id, ft.name FROM FacilityType ft ORDER BY ft.id")
    List<Object[]> findAllFacilityTypeOptions();
}