package com.group8.rbs.dto.facility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityNameOptionsResponse {
    private Long facilityTypeId;
    private String facilityType;
}
