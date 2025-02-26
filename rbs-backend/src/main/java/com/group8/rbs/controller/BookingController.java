package com.group8.rbs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.service.booking.BookingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    
    @GetMapping("/facilities")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<List<Facility>> getFacilities() {
        return ResponseEntity.ok(bookingService.getFacilities());
    }
}
