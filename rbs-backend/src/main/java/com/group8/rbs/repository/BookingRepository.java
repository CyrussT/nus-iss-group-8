package com.group8.rbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group8.rbs.entities.Facility;

@Repository
public interface BookingRepository extends JpaRepository<Facility, Long> {
    
}
