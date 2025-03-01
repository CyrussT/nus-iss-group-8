package com.group8.rbs.dto.facility;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacilityResponseDTO {
    private Long facilityId;
    private String resourceType;
    private String resourceName;
    private String location;
    private Integer capacity;
    private String message;
}
