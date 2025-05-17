package com.group8.rbs.controller;

import com.group8.rbs.dto.facility.*;
import com.group8.rbs.service.facility.*;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;


import java.util.Collections;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(FacilityController.class)
@AutoConfigureMockMvc(addFilters = false)
public class FacilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private FacilityService facilityService;

    @Autowired
    private ObjectMapper objectMapper;

    private FacilityResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new FacilityResponseDTO();
        sampleResponse.setFacilityId(1L);
        sampleResponse.setResourceTypeId(1L);
        sampleResponse.setLocation("Test City");
        sampleResponse.setCapacity(50);
        sampleResponse.setResourceName("Sample Facility");
    }

    @Test
    void testCreateFacility() throws Exception {
        FacilityRequestDTO request = new FacilityRequestDTO();
        request.setResourceName("New Facility");
        request.setResourceTypeId(1L);
        request.setLocation("Test City");
        request.setCapacity(50);
        
        Mockito.when(facilityService.createFacility(any())).thenReturn(sampleResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/facilities/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceName").value("Sample Facility"));
                
    }

    @Test
    void testGetAllFacilities() throws Exception {
        Page<FacilityResponseDTO> page = new PageImpl<>(Collections.singletonList(sampleResponse));
        Mockito.when(facilityService.getAllFacilities(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/facilities/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].resourceName").value("Sample Facility"));
    }

    @Test
    void testGetFacilityById() throws Exception {
        FacilityResponseDTO dto = new FacilityResponseDTO();
        dto.setFacilityId(1L);
        dto.setResourceName("Test Facility");

        Mockito.when(facilityService.getFacilityById(1L)).thenReturn(dto);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/facilities/details/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceName").value("Test Facility"));
    }
    @Test
    void testUpdateFacility() throws Exception {
        FacilityRequestDTO updateRequest = new FacilityRequestDTO();
        updateRequest.setResourceName("Updated Facility");

        Mockito.when(facilityService.updateFacility(eq(1L), any())).thenReturn(sampleResponse);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/facilities/update/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceName").value("Sample Facility"));
    }

    @Test
    void testDeleteFacility() throws Exception {
        Mockito.doNothing().when(facilityService).deleteFacility(1L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/facilities/delete/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testSearchFacilities() throws Exception {
        Page<FacilityResponseDTO> page = new PageImpl<>(Collections.singletonList(sampleResponse));
        Mockito.when(facilityService.searchFacilities(any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/facilities/search")
                .param("resourceName", "Sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].resourceName").value("Sample Facility"));
    }

    @Test
    void testGetFacilityWithBookings() throws Exception {
        Mockito.when(facilityService.getFacilityWithBookings(1L)).thenReturn(sampleResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/facilities/1/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceName").value("Sample Facility"));
    }
}