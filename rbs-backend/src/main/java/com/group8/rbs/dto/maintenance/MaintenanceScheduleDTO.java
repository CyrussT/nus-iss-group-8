package com.group8.rbs.dto.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceScheduleDTO {
    private Long maintenanceId;
    private String description;
    private String startDate;
    private String endDate;
    private String createdBy;
    private Long facilityId;
}