package com.group8.rbs.controller;

import com.group8.rbs.entities.FacilityType;
import com.group8.rbs.repository.FacilityTypeRepository;
import com.group8.rbs.security.JwtAuthFilter;
import com.group8.rbs.service.security.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FacilityTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
class FacilityTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FacilityTypeRepository facilityTypeRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void testGetAllFacilityTypes_returnsList() throws Exception {
        FacilityType type1 = new FacilityType();
        type1.setId(1L);
        type1.setName("Lecture Hall");

        FacilityType type2 = new FacilityType();
        type2.setId(2L);
        type2.setName("Lab");

        when(facilityTypeRepository.findAll()).thenReturn(List.of(type1, type2));

        mockMvc.perform(get("/api/facility-types/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Lecture Hall"))
                .andExpect(jsonPath("$[1].name").value("Lab"));
    }
}
