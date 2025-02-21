package com.group8.rbs.dto.facility;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class FacilityRequestDTO {

    @NotBlank(message = "Resource type cannot be empty")
    private String resourceType;

    @NotBlank(message = "Resource name cannot be empty")
    private String resourceName;

    @NotBlank(message = "Location cannot be empty")
    private String location;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
}