package com.group8.rbs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.MaintenanceSchedule;
import com.group8.rbs.repository.AccountRepository;
import com.group8.rbs.security.JwtAuthFilter;
import com.group8.rbs.service.email.CustomEmailService;
import com.group8.rbs.service.maintenance.MaintenanceService;
import com.group8.rbs.service.security.JwtService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MaintenanceController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable Spring Security filters for clean unit test
class MaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaintenanceService maintenanceService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private CustomEmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private JwtAuthFilter jwtAuthFilter;
    @MockBean private JwtService jwtService;

    @Test
    void testScheduleMaintenanceForFacility_successful() throws Exception {
        // Arrange input
        String startDate = LocalDate.now().plusDays(1).toString();
        String endDate = LocalDate.now().plusDays(2).toString();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("facilityId", 1L);
        requestMap.put("startDate", startDate);
        requestMap.put("endDate", endDate);
        requestMap.put("description", "Test maintenance");
        requestMap.put("createdBy", "admin@example.com");

        // Mocked account
        Account mockAccount = Account.builder()
                .accountId(1L)
                .name("Admin")
                .email("admin@example.com")
                .status("Active")
                .password("dummy")
                .accountType(null)
                .build();

        // Mocked maintenance schedule
        MaintenanceSchedule mockSchedule = new MaintenanceSchedule();
        mockSchedule.setFacilityId(1L);
        mockSchedule.setStartDate(startDate);
        mockSchedule.setEndDate(endDate);
        mockSchedule.setDescription("Test maintenance");
        mockSchedule.setCreatedBy(1L);

        // Define service behaviors
        when(accountRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(mockAccount));

        when(maintenanceService.findBookingsAffectedByMaintenance(anyLong(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        when(maintenanceService.scheduleMaintenanceForFacility(any())).thenReturn(mockSchedule);

        // Act & Assert
        mockMvc.perform(post("/api/maintenance/schedule")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestMap)))
            .andDo(print()) // 👈 Will show response in the console
            .andExpect(status().isCreated()) // Will fail if not 201
            .andExpect(jsonPath("$.facilityId").value(1))
            .andExpect(jsonPath("$.description").value("Test maintenance"));

    }
}
