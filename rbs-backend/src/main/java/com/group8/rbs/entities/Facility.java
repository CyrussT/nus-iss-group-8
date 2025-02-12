package com.group8.rbs.entities;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "TBL_FACILITY")
@Getter
@Setter
public class Facility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FACILITY_ID")
    private Long facilityId;

    @Column(name = "RESOURCE_TYPE", nullable = false)
    private String resourceType;

    @Column(name = "RESOURCE_NAME", nullable = false)
    private String resourceName;

    @Column(name = "LOCATION")
    private String location;

    @Column(name = "CAPACITY")
    private Integer capacity;

    @OneToMany(mappedBy = "facility")
    private List<Booking> bookings;
}
