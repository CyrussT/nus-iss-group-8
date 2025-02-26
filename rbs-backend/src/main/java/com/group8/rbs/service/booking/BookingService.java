package com.group8.rbs.service.booking;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group8.rbs.entities.Facility;
import com.group8.rbs.repository.BookingRepository;
import com.group8.rbs.repository.FacilityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final FacilityRepository facilityRepository;

    public List<Facility> getFacilities() {
        return facilityRepository.findAll();
    }
}
