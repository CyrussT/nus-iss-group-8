package com.group8.rbs.entities;
import jakarta.persistence.*;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "TBL_FACILITY", indexes = {
    @Index(name = "idx_resource_type", columnList = "RESOURCE_TYPE"),
    @Index(name = "idx_resource_name", columnList = "RESOURCE_NAME"),
    @Index(name = "idx_location", columnList = "LOCATION"),
    @Index(name = "idx_capacity", columnList = "CAPACITY")
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Facility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FACILITY_ID")
    private Long facilityId;

    @Column(name = "RESOURCE_TYPE", nullable = false)
    private Long resourceTypeId;

    @Column(name = "RESOURCE_NAME", nullable = false)
    private String resourceName;

    @Column(name = "LOCATION")
    private String location;

    @Column(name = "CAPACITY")
    private Integer capacity;

    @OneToMany(mappedBy = "facility")
    @JsonBackReference
    private List<Booking> bookings;
}
