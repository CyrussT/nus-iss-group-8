package com.group8.rbs.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TBL_MAINTENANCE_SCHEDULE")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MaintenanceSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MAINTENANCE_ID")
    private Long maintenanceId;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "END_DATE", nullable = false)
    private String endDate;

    @Column(name = "START_DATE", nullable = false)
    private String startDate;

    @Column(name = "CREATED_BY", nullable = false)
    private Long createdBy;

    @Column(name = "FACILITY_ID", nullable = false)
    private Long facilityId;
}