package com.group8.rbs.dto.facility;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacilitySearchDTO {
    private Long resourceTypeId;
    private String resourceName;
    private String location;
    private Integer capacity;
}
