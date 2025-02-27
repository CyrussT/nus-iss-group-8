package com.group8.rbs.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitySearchDTO {
    private String resourceType;
    private String resourceName;
    private String location;
    private Integer capacity;
}