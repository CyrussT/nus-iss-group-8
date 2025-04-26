package com.group8.rbs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group8.rbs.config.TestConfig;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Booking;
import com.group8.rbs.entities.Facility;
import com.group8.rbs.entities.MaintenanceSchedule;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.repository.AccountRepository;
import com.group8.rbs.security.CustomUserDetailsService;
import com.group8.rbs.security.SecurityConfig;
import com.group8.rbs.service.email.CustomEmailService;
import com.group8.rbs.service.maintenance.MaintenanceService;
import com.group8.rbs.service.security.AuthService;
import com.group8.rbs.service.security.JwtService;

@WebMvcTest(MaintenanceController.class)
@Import({SecurityConfig.class, TestConfig.class})
@ActiveProfiles("test")
public class MaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaintenanceService maintenanceService;
    
    @MockBean
    private AccountRepository accountRepository;
    
    @MockBean
    private CustomEmailService emailService;
    
    @MockBean
    private JwtService jwtService;
    
    @MockBean
    private AuthService authService;
    
    @MockBean 
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private MaintenanceSchedule maintenanceSchedule;
    private List<Booking> affectedBookings;
    private Account testAccount;
    private Map<String, Object> maintenanceRequest;

    @BeforeEach
    void setUp() {
        // Setup MaintenanceSchedule - use a future end date (not today)
        maintenanceSchedule = MaintenanceSchedule.builder()
                .maintenanceId(1L)
                .facilityId(1L)
                .startDate(LocalDate.now().toString())
                .endDate(LocalDate.now().plusDays(5).toString())
                .description("Test Maintenance")
                .createdBy(1L)
                .build();
        
        // Setup test account
        testAccount = Account.builder()
                .accountId(1L)
                .email("admin@example.com")
                .name("Admin User")
                .build();
        
        // Setup a facility
        Facility facility = Facility.builder()
                .facilityId(1L)
                .resourceName("Test Facility")
                .location("Test Location")
                .build();
        
        // Setup a booking
        Booking booking = Booking.builder()
                .bookingId(1L)
                .facility(facility)
                .account(testAccount)
                .bookedDateTime(LocalDate.now().atTime(10, 0))
                .timeSlot("10:00 - 11:00")
                .status(BookingStatus.APPROVED)
                .build();
        
        affectedBookings = Arrays.asList(booking);
        
        // Setup maintenance request for API calls
        maintenanceRequest = new HashMap<>();
        maintenanceRequest.put("facilityId", 1L);
        maintenanceRequest.put("startDate", LocalDate.now().toString());
        maintenanceRequest.put("endDate", LocalDate.now().plusDays(5).toString());
        maintenanceRequest.put("description", "Test Maintenance");
        maintenanceRequest.put("createdBy", "admin@example.com");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMINISTRATOR"})
    void scheduleMaintenanceForFacilityShouldReturnCreatedSchedule() throws Exception {
        // Mock account repository
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(testAccount));
        
        // Mock maintenance service - first mock find bookings
        when(maintenanceService.findBookingsAffectedByMaintenance(anyLong(), anyString(), anyString()))
            .thenReturn(Arrays.asList());
        
        // Then mock schedule maintenance
        when(maintenanceService.scheduleMaintenanceForFacility(any(MaintenanceSchedule.class)))
            .thenReturn(maintenanceSchedule);
        
        // Call the endpoint
        mockMvc.perform(post("/api/maintenance/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maintenanceRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maintenanceId").value(1))
                .andExpect(jsonPath("$.facilityId").value(1));
    }
    
    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMINISTRATOR"})
    void getAffectedBookingsCountShouldReturnCount() throws Exception {
        // Mock maintenance service
        when(maintenanceService.findBookingsAffectedByMaintenance(anyLong(), anyString(), anyString()))
            .thenReturn(affectedBookings);
        
        // Call the endpoint
        mockMvc.perform(get("/api/maintenance/affected-bookings")
                .param("facilityId", "1")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().plusDays(5).toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.bookings").isArray())
                .andExpect(jsonPath("$.bookings[0].bookingId").value(1));
    }
    
    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMINISTRATOR"})
    void releaseFacilityFromMaintenanceShouldUpdateEndDate() throws Exception {
        // Create a maintenance schedule with a future end date (not today)
        LocalDate futureDate = LocalDate.now().plusDays(5);
        MaintenanceSchedule futureMaintenanceSchedule = MaintenanceSchedule.builder()
                .maintenanceId(1L)
                .facilityId(1L)
                .startDate(LocalDate.now().toString())
                .endDate(futureDate.toString()) // Important: Must be a future date, not today
                .description("Test Maintenance")
                .createdBy(1L)
                .build();
        
        // Mock maintenance service to return a maintenance schedule with future end date
        when(maintenanceService.getCurrentMaintenanceForFacility(anyLong()))
            .thenReturn(Optional.of(futureMaintenanceSchedule));
        
        // Mock update end date to return schedule with today's date
        MaintenanceSchedule updatedSchedule = MaintenanceSchedule.builder()
                .maintenanceId(1L)
                .facilityId(1L)
                .startDate(LocalDate.now().toString())
                .endDate(LocalDate.now().toString()) // End date is today (early release)
                .description("Test Maintenance")
                .createdBy(1L)
                .build();
                
        when(maintenanceService.updateMaintenanceEndDate(anyLong(), anyString()))
            .thenReturn(updatedSchedule);
        
        // Call the endpoint
        mockMvc.perform(post("/api/maintenance/release/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate").value(LocalDate.now().toString()));
    }
    
    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMINISTRATOR"})
    void getCurrentMaintenanceForFacilityShouldReturnMaintenanceSchedule() throws Exception {
        // Mock maintenance service
        when(maintenanceService.getCurrentMaintenanceForFacility(anyLong()))
            .thenReturn(Optional.of(maintenanceSchedule));
        
        // Call the endpoint
        mockMvc.perform(get("/api/maintenance/current/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceId").value(1))
                .andExpect(jsonPath("$.facilityId").value(1));
    }
    
    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMINISTRATOR"})
    void checkMaintenanceStatusBatchShouldReturnStatusMap() throws Exception {
        // Create request body
        Map<String, Object> request = new HashMap<>();
        request.put("facilityIds", Arrays.asList(1L, 2L, 3L));
        request.put("date", LocalDate.now().toString());
        
        // Create mock response
        Map<String, Boolean> statusMap = new HashMap<>();
        statusMap.put("1", true);
        statusMap.put("2", false);
        statusMap.put("3", false);
        
        // Mock maintenance service
        when(maintenanceService.checkMaintenanceStatusBatch(anyList(), any(LocalDate.class)))
            .thenReturn(statusMap);
        
        // Call the endpoint
        mockMvc.perform(post("/api/maintenance/check-maintenance-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.1").value(true))
                .andExpect(jsonPath("$.2").value(false))
                .andExpect(jsonPath("$.3").value(false));
    }
    
    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMINISTRATOR"})
    void isFacilityUnderMaintenanceShouldReturnStatus() throws Exception {
        // Mock maintenance service
        when(maintenanceService.isFacilityUnderMaintenance(anyLong()))
            .thenReturn(true);
        
        // Call the endpoint
        mockMvc.perform(get("/api/maintenance/check/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
    
    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMINISTRATOR"})
    void getAllMaintenanceSchedulesShouldReturnList() throws Exception {
        // Mock maintenance service
        when(maintenanceService.getAllMaintenanceSchedules())
            .thenReturn(Arrays.asList(maintenanceSchedule));
        
        // Call the endpoint
        mockMvc.perform(get("/api/maintenance/all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].maintenanceId").value(1));
    }
    
    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMINISTRATOR"})
    void getMaintenanceSchedulesForFacilityShouldReturnList() throws Exception {
        // Mock maintenance service
        when(maintenanceService.getMaintenanceSchedulesForFacility(anyLong()))
            .thenReturn(Arrays.asList(maintenanceSchedule));
        
        // Call the endpoint
        mockMvc.perform(get("/api/maintenance/facility/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].maintenanceId").value(1));
    }
    
    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMINISTRATOR"})
    void getMaintenanceScheduleByIdShouldReturnSchedule() throws Exception {
        // Mock maintenance service
        when(maintenanceService.getMaintenanceScheduleById(anyLong()))
            .thenReturn(Optional.of(maintenanceSchedule));
        
        // Call the endpoint
        mockMvc.perform(get("/api/maintenance/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceId").value(1));
    }
}