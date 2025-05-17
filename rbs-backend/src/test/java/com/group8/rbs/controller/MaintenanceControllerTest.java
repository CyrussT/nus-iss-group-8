package com.group8.rbs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
import com.group8.rbs.exception.MaintenanceOverlapException;
import com.group8.rbs.repository.AccountRepository;
import com.group8.rbs.security.SecurityConfig;
import com.group8.rbs.service.email.EmailServiceFactory;
import com.group8.rbs.service.maintenance.MaintenanceService;

@WebMvcTest(MaintenanceController.class)
@Import({ SecurityConfig.class, TestConfig.class })
@ActiveProfiles("test")
public class MaintenanceControllerTest {

        @Autowired
        private MockMvc mockMvc;

        private MaintenanceService maintenanceService;

        private AccountRepository accountRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private MaintenanceSchedule maintenanceSchedule;
        private List<Booking> affectedBookings;
        private Account testAccount;
        private Facility testFacility;
        private Map<String, Object> maintenanceRequest;
        private Booking testBooking;

        @BeforeEach
        void setUp() {
                // Setup MaintenanceSchedule - use a future end date (not today)
                LocalDate today = LocalDate.now();
                LocalDate futureDate = today.plusDays(5);

                maintenanceSchedule = MaintenanceSchedule.builder()
                                .maintenanceId(1L)
                                .facilityId(1L)
                                .startDate(today.toString())
                                .endDate(futureDate.toString())
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
                testFacility = Facility.builder()
                                .facilityId(1L)
                                .resourceName("Test Facility")
                                .location("Test Location")
                                .build();

                // Setup a booking
                testBooking = Booking.builder()
                                .bookingId(1L)
                                .facility(testFacility)
                                .account(testAccount)
                                .bookedDateTime(LocalDate.now().atTime(10, 0))
                                .timeSlot("10:00 - 11:00")
                                .status(BookingStatus.APPROVED)
                                .build();

                affectedBookings = Arrays.asList(testBooking);

                // Setup maintenance request for API calls
                maintenanceRequest = new HashMap<>();
                maintenanceRequest.put("facilityId", 1L);
                maintenanceRequest.put("startDate", today.toString());
                maintenanceRequest.put("endDate", futureDate.toString());
                maintenanceRequest.put("description", "Test Maintenance");
                maintenanceRequest.put("createdBy", "admin@example.com");
        }

        @Test
        @DisplayName("Schedule maintenance for facility should return created schedule")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void scheduleMaintenanceForFacilityShouldReturnCreatedSchedule() throws Exception {
                // Mock account repository
                when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(testAccount));

                // Mock maintenance service - first mock find bookings
                when(maintenanceService.findBookingsAffectedByMaintenance(
                                anyLong(), anyString(), anyString(), any(LocalDateTime.class)))
                                .thenReturn(Collections.emptyList());

                // Then mock schedule maintenance
                when(maintenanceService.scheduleMaintenanceForFacility(any(MaintenanceSchedule.class)))
                                .thenReturn(maintenanceSchedule);

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(maintenanceRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.maintenanceId").value(1))
                                .andExpect(jsonPath("$.facilityId").value(1))
                                .andExpect(jsonPath("$.description").value("Test Maintenance"));

                verify(maintenanceService, times(1)).scheduleMaintenanceForFacility(any(MaintenanceSchedule.class));
        }

        @Test
        @DisplayName("Schedule maintenance should handle affected bookings")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void scheduleMaintenanceShouldHandleAffectedBookings() throws Exception {
                // Mock account repository
                when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(testAccount));

                // Mock maintenance service - return affected bookings
                when(maintenanceService.findBookingsAffectedByMaintenance(
                                anyLong(), anyString(), anyString(), any(LocalDateTime.class)))
                                .thenReturn(affectedBookings);

                // Mock schedule maintenance
                when(maintenanceService.scheduleMaintenanceForFacility(any(MaintenanceSchedule.class)))
                                .thenReturn(maintenanceSchedule);

                // Mock cancelling bookings - return count of cancelled
                when(maintenanceService.cancelBookingsForMaintenance(
                                anyList(), any(MaintenanceSchedule.class), any(EmailServiceFactory.class)))
                                .thenReturn(1);

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(maintenanceRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.maintenanceSchedule.maintenanceId").value(1))
                                .andExpect(jsonPath("$.cancelledBookings").value(1));

                verify(maintenanceService, times(1)).scheduleMaintenanceForFacility(any(MaintenanceSchedule.class));
                verify(maintenanceService, times(1)).cancelBookingsForMaintenance(
                                anyList(), any(MaintenanceSchedule.class), any(EmailServiceFactory.class));
        }

        @Test
        @DisplayName("Schedule maintenance should validate required fields")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void scheduleMaintenanceShouldValidateRequiredFields() throws Exception {
                // Remove required fields from request
                Map<String, Object> invalidRequest = new HashMap<>();
                invalidRequest.put("description", "Test Maintenance");

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Schedule maintenance should validate date formats")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void scheduleMaintenanceShouldValidateDateFormats() throws Exception {
                // Create request with invalid date format
                Map<String, Object> invalidRequest = new HashMap<>(maintenanceRequest);
                invalidRequest.put("startDate", "01/01/2023"); // Wrong format

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error")
                                                .value(org.hamcrest.Matchers.containsString("Invalid date format")));
        }

        @Test
        @DisplayName("Schedule maintenance should validate start date not after end date")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void scheduleMaintenanceShouldValidateStartDateNotAfterEndDate() throws Exception {
                // Create request with start date after end date
                Map<String, Object> invalidRequest = new HashMap<>(maintenanceRequest);
                invalidRequest.put("startDate", LocalDate.now().plusDays(10).toString());
                invalidRequest.put("endDate", LocalDate.now().plusDays(5).toString());

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error")
                                                .value(org.hamcrest.Matchers.containsString(
                                                                "Start date cannot be after end date")));
        }

        @Test
        @DisplayName("Schedule maintenance should validate start date not in past")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void scheduleMaintenanceShouldValidateStartDateNotInPast() throws Exception {
                // Create request with start date in past
                Map<String, Object> invalidRequest = new HashMap<>(maintenanceRequest);
                invalidRequest.put("startDate", LocalDate.now().minusDays(1).toString());

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error")
                                                .value(org.hamcrest.Matchers
                                                                .containsString("Start date cannot be in the past")));
        }

        @Test
        @DisplayName("Schedule maintenance should handle maintenance overlap exception")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void scheduleMaintenanceShouldHandleMaintenanceOverlapException() throws Exception {
                // Mock account repository
                when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(testAccount));

                // Mock maintenance service - empty affected bookings
                when(maintenanceService.findBookingsAffectedByMaintenance(
                                anyLong(), anyString(), anyString(), any(LocalDateTime.class)))
                                .thenReturn(Collections.emptyList());

                // Mock schedule maintenance to throw overlap exception
                when(maintenanceService.scheduleMaintenanceForFacility(any(MaintenanceSchedule.class)))
                                .thenThrow(new MaintenanceOverlapException(
                                                "This facility already has scheduled maintenance"));

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(maintenanceRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error")
                                                .value(org.hamcrest.Matchers
                                                                .containsString("already has scheduled maintenance")));
        }

        @Test
        @DisplayName("Get affected bookings count should return count and bookings")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getAffectedBookingsCountShouldReturnCountAndBookings() throws Exception {
                // Mock maintenance service
                when(maintenanceService.findBookingsAffectedByMaintenance(
                                anyLong(), anyString(), anyString(), any(LocalDateTime.class)))
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
                                .andExpect(jsonPath("$.bookings[0].bookingId").value(1))
                                .andExpect(jsonPath("$.bookings[0].timeSlot").value("10:00 - 11:00"));
        }

        @Test
        @DisplayName("Get affected bookings count should handle empty bookings")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getAffectedBookingsCountShouldHandleEmptyBookings() throws Exception {
                // Mock maintenance service
                when(maintenanceService.findBookingsAffectedByMaintenance(
                                anyLong(), anyString(), anyString(), any(LocalDateTime.class)))
                                .thenReturn(Collections.emptyList());

                // Call the endpoint
                mockMvc.perform(get("/api/maintenance/affected-bookings")
                                .param("facilityId", "1")
                                .param("startDate", LocalDate.now().toString())
                                .param("endDate", LocalDate.now().plusDays(5).toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.count").value(0))
                                .andExpect(jsonPath("$.bookings").isArray())
                                .andExpect(jsonPath("$.bookings").isEmpty());
        }

        @Test
        @DisplayName("Release facility from maintenance should update end date")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
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

                // Mock maintenance service to return a maintenance schedule with future end
                // date
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

                verify(maintenanceService, times(1)).updateMaintenanceEndDate(eq(1L), eq(LocalDate.now().toString()));
        }

        @Test
        @DisplayName("Release facility should handle no current maintenance")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void releaseFacilityShouldHandleNoCurrentMaintenance() throws Exception {
                // Mock maintenance service to return empty optional (no current maintenance)
                when(maintenanceService.getCurrentMaintenanceForFacility(anyLong()))
                                .thenReturn(Optional.empty());

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/release/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error")
                                                .value("This facility is not currently under maintenance"));
        }

        @Test
        @DisplayName("Release facility should handle already ending today")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void releaseFacilityShouldHandleAlreadyEndingToday() throws Exception {
                // Create a maintenance schedule that already ends today
                MaintenanceSchedule todayEndSchedule = MaintenanceSchedule.builder()
                                .maintenanceId(1L)
                                .facilityId(1L)
                                .startDate(LocalDate.now().minusDays(1).toString())
                                .endDate(LocalDate.now().toString()) // Already ends today
                                .description("Test Maintenance")
                                .createdBy(1L)
                                .build();

                // Mock maintenance service
                when(maintenanceService.getCurrentMaintenanceForFacility(anyLong()))
                                .thenReturn(Optional.of(todayEndSchedule));

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/release/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Facility maintenance is already ending today"));
        }

        @Test
        @DisplayName("Get current maintenance for facility should return maintenance schedule")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getCurrentMaintenanceForFacilityShouldReturnMaintenanceSchedule() throws Exception {
                // Mock maintenance service
                when(maintenanceService.getCurrentMaintenanceForFacility(anyLong()))
                                .thenReturn(Optional.of(maintenanceSchedule));

                // Call the endpoint
                mockMvc.perform(get("/api/maintenance/current/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.maintenanceId").value(1))
                                .andExpect(jsonPath("$.facilityId").value(1))
                                .andExpect(jsonPath("$.description").value("Test Maintenance"));
        }

        @Test
        @DisplayName("Get current maintenance for facility should handle no maintenance")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getCurrentMaintenanceForFacilityShouldHandleNoMaintenance() throws Exception {
                // Mock maintenance service to return empty optional
                when(maintenanceService.getCurrentMaintenanceForFacility(anyLong()))
                                .thenReturn(Optional.empty());

                // Call the endpoint
                mockMvc.perform(get("/api/maintenance/current/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().string("")); // Changed from "null" to empty string
        }

        @Test
        @DisplayName("Get current maintenance for facility with date param should work")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getCurrentMaintenanceForFacilityWithDateParamShouldWork() throws Exception {
                // Mock maintenance service - getMaintenanceForFacilityOnDate
                when(maintenanceService.getMaintenanceForFacilityOnDate(anyLong(), any(LocalDate.class)))
                                .thenReturn(Optional.of(maintenanceSchedule));

                // Call the endpoint with date parameter
                mockMvc.perform(get("/api/maintenance/current/1")
                                .param("date", LocalDate.now().plusDays(1).toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.maintenanceId").value(1));

                verify(maintenanceService, times(1)).getMaintenanceForFacilityOnDate(eq(1L), any(LocalDate.class));
        }

        @Test
        @DisplayName("Check maintenance status batch should return status map")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
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
        @DisplayName("Check maintenance status batch with no date should use current date")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void checkMaintenanceStatusBatchWithNoDateShouldUseCurrentDate() throws Exception {
                // Create request body without date
                Map<String, Object> request = new HashMap<>();
                request.put("facilityIds", Arrays.asList(1L, 2L, 3L));

                // Create mock response - empty map
                Map<String, Boolean> statusMap = new HashMap<>();

                // Mock maintenance service - should handle null date parameter
                when(maintenanceService.checkMaintenanceStatusBatch(anyList(), eq(null)))
                                .thenReturn(statusMap);

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/check-maintenance-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // Verify service was called with null date parameter
                verify(maintenanceService, times(1)).checkMaintenanceStatusBatch(anyList(), eq(null));
        }

        @Test
        @DisplayName("Is facility under maintenance should return status")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
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
        @DisplayName("Is facility under maintenance with date should check for date")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void isFacilityUnderMaintenanceWithDateShouldCheckForDate() throws Exception {
                // Mock maintenance service
                when(maintenanceService.isFacilityUnderMaintenanceOnDate(anyLong(), any(LocalDate.class)))
                                .thenReturn(false);

                // Call the endpoint with date parameter
                mockMvc.perform(get("/api/maintenance/check/1")
                                .param("date", LocalDate.now().plusDays(10).toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().string("false"));

                verify(maintenanceService, times(1)).isFacilityUnderMaintenanceOnDate(eq(1L), any(LocalDate.class));
        }

        @Test
        @DisplayName("Is facility under maintenance with invalid date format should use current date")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void isFacilityUnderMaintenanceWithInvalidDateFormatShouldUseCurrentDate() throws Exception {
                // Mock maintenance service
                when(maintenanceService.isFacilityUnderMaintenance(anyLong()))
                                .thenReturn(true);

                // Call the endpoint with invalid date format
                mockMvc.perform(get("/api/maintenance/check/1")
                                .param("date", "invalid-date-format")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().string("true"));

                verify(maintenanceService, times(1)).isFacilityUnderMaintenance(eq(1L));
        }

        @Test
        @DisplayName("Get all maintenance schedules should return list")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getAllMaintenanceSchedulesShouldReturnList() throws Exception {
                // Mock maintenance service
                when(maintenanceService.getAllMaintenanceSchedules())
                                .thenReturn(Arrays.asList(maintenanceSchedule));

                // Call the endpoint
                mockMvc.perform(get("/api/maintenance/all")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].maintenanceId").value(1))
                                .andExpect(jsonPath("$[0].description").value("Test Maintenance"));
        }

        @Test
        @DisplayName("Get maintenance schedules for facility should return list")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getMaintenanceSchedulesForFacilityShouldReturnList() throws Exception {
                // Mock maintenance service
                when(maintenanceService.getMaintenanceSchedulesForFacility(anyLong()))
                                .thenReturn(Arrays.asList(maintenanceSchedule));

                // Call the endpoint
                mockMvc.perform(get("/api/maintenance/facility/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].maintenanceId").value(1))
                                .andExpect(jsonPath("$[0].facilityId").value(1));
        }

        @Test
        @DisplayName("Get maintenance schedule by id should return schedule")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getMaintenanceScheduleByIdShouldReturnSchedule() throws Exception {
                // Mock maintenance service
                when(maintenanceService.getMaintenanceScheduleById(anyLong()))
                                .thenReturn(Optional.of(maintenanceSchedule));

                // Call the endpoint
                mockMvc.perform(get("/api/maintenance/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.maintenanceId").value(1))
                                .andExpect(jsonPath("$.facilityId").value(1))
                                .andExpect(jsonPath("$.description").value("Test Maintenance"));
        }

        @Test
        @DisplayName("Get maintenance schedule by id should handle not found")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getMaintenanceScheduleByIdShouldHandleNotFound() throws Exception {
                // Mock maintenance service
                when(maintenanceService.getMaintenanceScheduleById(anyLong()))
                                .thenReturn(Optional.empty());

                // Call the endpoint
                mockMvc.perform(get("/api/maintenance/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound())
                                .andExpect(content().string("Maintenance schedule not found"));
        }

        @Test
        @DisplayName("Schedule maintenance should handle service exceptions")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void scheduleMaintenanceShouldHandleServiceExceptions() throws Exception {
                // Mock account repository
                when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(testAccount));

                // Mock maintenance service - empty affected bookings
                when(maintenanceService.findBookingsAffectedByMaintenance(
                                anyLong(), anyString(), anyString(), any(LocalDateTime.class)))
                                .thenReturn(Collections.emptyList());

                // Mock schedule maintenance to throw an unexpected exception
                when(maintenanceService.scheduleMaintenanceForFacility(any(MaintenanceSchedule.class)))
                                .thenThrow(new RuntimeException("Unexpected server error"));

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(maintenanceRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error")
                                                .value(org.hamcrest.Matchers.containsString(
                                                                "An error occurred: Unexpected server error")));
        }

        @Test
        @DisplayName("Get affected bookings should handle missing parameters")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getAffectedBookingsShouldHandleMissingParameters() throws Exception {
                // Call the endpoint without required parameters
                mockMvc.perform(get("/api/maintenance/affected-bookings")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Get affected bookings should handle service exception")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getAffectedBookingsShouldHandleServiceException() throws Exception {
                // Mock maintenance service to throw exception
                when(maintenanceService.findBookingsAffectedByMaintenance(
                                anyLong(), anyString(), anyString(), any(LocalDateTime.class)))
                                .thenThrow(new RuntimeException("Service error"));

                // Call the endpoint
                mockMvc.perform(get("/api/maintenance/affected-bookings")
                                .param("facilityId", "1")
                                .param("startDate", LocalDate.now().toString())
                                .param("endDate", LocalDate.now().plusDays(5).toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error")
                                                .value(org.hamcrest.Matchers.containsString(
                                                                "Failed to get affected bookings: Service error")));
        }

        @Test
        @DisplayName("Release facility should handle service exception")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void releaseFacilityShouldHandleServiceException() throws Exception {
                // Mock maintenance service to throw exception when getting current maintenance
                when(maintenanceService.getCurrentMaintenanceForFacility(anyLong()))
                                .thenThrow(new RuntimeException("Service error"));

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/release/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error")
                                                .value(org.hamcrest.Matchers.containsString(
                                                                "Failed to release facility: Service error")));
        }

        @Test
        @DisplayName("Release facility should handle update end date exception")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void releaseFacilityShouldHandleUpdateEndDateException() throws Exception {
                // Create a maintenance schedule with a future end date
                LocalDate futureDate = LocalDate.now().plusDays(5);
                MaintenanceSchedule futureMaintenanceSchedule = MaintenanceSchedule.builder()
                                .maintenanceId(1L)
                                .facilityId(1L)
                                .startDate(LocalDate.now().toString())
                                .endDate(futureDate.toString())
                                .description("Test Maintenance")
                                .createdBy(1L)
                                .build();

                // Mock maintenance service to return maintenance but throw on update
                when(maintenanceService.getCurrentMaintenanceForFacility(anyLong()))
                                .thenReturn(Optional.of(futureMaintenanceSchedule));

                when(maintenanceService.updateMaintenanceEndDate(anyLong(), anyString()))
                                .thenThrow(new RuntimeException("Update failed"));

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/release/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error")
                                                .value(org.hamcrest.Matchers.containsString(
                                                                "Failed to release facility: Update failed")));
        }

        @Test
        @DisplayName("Get current maintenance should handle service exception")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getCurrentMaintenanceShouldHandleServiceException() throws Exception {
                // Mock maintenance service to throw exception
                when(maintenanceService.getCurrentMaintenanceForFacility(anyLong()))
                                .thenThrow(new RuntimeException("Service error"));

                // Call the endpoint
                mockMvc.perform(get("/api/maintenance/current/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value(
                                                org.hamcrest.Matchers.containsString(
                                                                "Failed to get current maintenance: Service error")));
        }

        @Test
        @DisplayName("Check maintenance status batch with empty facility IDs should return empty map")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void checkMaintenanceStatusBatchWithEmptyFacilityIdsShouldReturnEmptyMap() throws Exception {
                // Create request body with empty facilityIds
                Map<String, Object> request = new HashMap<>();
                request.put("facilityIds", Collections.emptyList());

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/check-maintenance-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Check maintenance status batch should handle service exception")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void checkMaintenanceStatusBatchShouldHandleServiceException() throws Exception {
                // Create request body
                Map<String, Object> request = new HashMap<>();
                request.put("facilityIds", Arrays.asList(1L, 2L, 3L));

                // Mock service to throw exception
                when(maintenanceService.checkMaintenanceStatusBatch(anyList(), any(LocalDate.class)))
                                .thenThrow(new RuntimeException("Service error"));

                // Call the endpoint and expect empty map response on error
                mockMvc.perform(post("/api/maintenance/check-maintenance-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Get all maintenance schedules should handle empty list")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getAllMaintenanceSchedulesShouldHandleEmptyList() throws Exception {
                // Mock maintenance service to return empty list
                when(maintenanceService.getAllMaintenanceSchedules())
                                .thenReturn(Collections.emptyList());

                // Call the endpoint
                mockMvc.perform(get("/api/maintenance/all")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Get maintenance schedules for facility should handle empty list")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void getMaintenanceSchedulesForFacilityShouldHandleEmptyList() throws Exception {
                // Mock maintenance service to return empty list
                when(maintenanceService.getMaintenanceSchedulesForFacility(anyLong()))
                                .thenReturn(Collections.emptyList());

                // Call the endpoint
                mockMvc.perform(get("/api/maintenance/facility/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Schedule maintenance should handle non-email createdBy value")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void scheduleMaintenanceShouldHandleNonEmailCreatedByValue() throws Exception {
                // Create request with non-email createdBy value
                Map<String, Object> nonEmailRequest = new HashMap<>(maintenanceRequest);
                nonEmailRequest.put("createdBy", "admin-user-id"); // Not an email

                // Mock find bookings to return empty list
                when(maintenanceService.findBookingsAffectedByMaintenance(
                                anyLong(), anyString(), anyString(), any(LocalDateTime.class)))
                                .thenReturn(Collections.emptyList());

                // Mock schedule maintenance
                when(maintenanceService.scheduleMaintenanceForFacility(any(MaintenanceSchedule.class)))
                                .thenReturn(maintenanceSchedule);

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(nonEmailRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.maintenanceId").value(1));

                // Should use default admin ID
                verify(maintenanceService, times(1)).scheduleMaintenanceForFacility(any(MaintenanceSchedule.class));
        }

        @Test
        @DisplayName("Schedule maintenance should handle null createdBy value")
        @WithMockUser(username = "admin@example.com", roles = { "ADMINISTRATOR" })
        void scheduleMaintenanceShouldHandleNullCreatedByValue() throws Exception {
                // Create request with null createdBy value
                Map<String, Object> nullCreatedByRequest = new HashMap<>(maintenanceRequest);
                nullCreatedByRequest.put("createdBy", null);

                // Mock find bookings to return empty list
                when(maintenanceService.findBookingsAffectedByMaintenance(
                                anyLong(), anyString(), anyString(), any(LocalDateTime.class)))
                                .thenReturn(Collections.emptyList());

                // Mock schedule maintenance
                when(maintenanceService.scheduleMaintenanceForFacility(any(MaintenanceSchedule.class)))
                                .thenReturn(maintenanceSchedule);

                // Call the endpoint
                mockMvc.perform(post("/api/maintenance/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(nullCreatedByRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.maintenanceId").value(1));

                // Should use default admin ID
                verify(maintenanceService, times(1)).scheduleMaintenanceForFacility(any(MaintenanceSchedule.class));
        }
}