package com.group8.rbs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.dto.booking.BookingRequestDTO;
import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.security.CustomUserDetailsService;
import com.group8.rbs.service.security.JwtService;
import com.group8.rbs.service.security.AuthService;
import com.group8.rbs.security.SecurityConfig;
import com.group8.rbs.service.booking.BookingService;
import com.group8.rbs.service.email.EmailContentStrategy;
import com.group8.rbs.service.email.EmailContentStrategyFactory;
import com.group8.rbs.service.email.EmailService;
import com.group8.rbs.service.email.EmailServiceFactory;

import jakarta.mail.MessagingException;

@WebMvcTest(BookingController.class)
@Import({ SecurityConfig.class, TestConfig.class })
@ActiveProfiles("test")
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private EmailServiceFactory emailServiceFactory;

    @MockBean
    private EmailContentStrategyFactory emailContentStrategyFactory;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AuthService authService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private EmailService emailService = mock(EmailService.class);
    private EmailContentStrategy emailContentStrategy = mock(EmailContentStrategy.class);

    private BookingDTO bookingDTO;
    private BookingResponseDTO bookingResponseDTO;
    private FacilitySearchDTO facilitySearchDTO;
    private List<FacilitySearchDTO> facilitySearchResults;
    private List<BookingResponseDTO> bookingList;
    private BookingRequestDTO bookingRequestDTO;

    @BeforeEach
    void setUp() {
        // Setup BookingDTO
        bookingDTO = new BookingDTO();
        bookingDTO.setFacilityId(1L);
        bookingDTO.setAccountEmail("test@example.com");
        bookingDTO.setBookedDateTime(LocalDateTime.now().plusDays(1));
        bookingDTO.setTimeSlot("14:00 - 15:00");
        bookingDTO.setTitle("Test Booking");
        bookingDTO.setDescription("Test Description");
        bookingDTO.setCreditsUsed("60");

        // Setup BookingResponseDTO
        bookingResponseDTO = BookingResponseDTO.builder()
                .bookingId(1L)
                .studentId("1")
                .studentName("Test Student")
                .facilityName("Test Facility")
                .location("Test Location")
                .bookedDatetime(LocalDateTime.now().plusDays(1))
                .timeslot("14:00 - 15:00")
                .status("APPROVED")
                .email("test@example.com")
                .title("Test Booking")
                .description("Test Description")
                .build();

        // Setup FacilitySearchDTO
        facilitySearchDTO = FacilitySearchDTO.builder()
                .facilityId(1L)
                .resourceTypeId(1L)
                .resourceName("Test Facility")
                .location("Test Location")
                .capacity(10)
                .date(LocalDate.now())
                .build();

        facilitySearchResults = Arrays.asList(facilitySearchDTO);

        bookingList = Arrays.asList(bookingResponseDTO);

        // Setup BookingRequestDTO
        bookingRequestDTO = BookingRequestDTO.builder()
                .studentId(1L)
                .status("APPROVED")
                .build();
    }

    @Test
    @DisplayName("Search facilities should return facility list")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void searchFacilitiesShouldReturnFacilityList() throws Exception {
        when(bookingService.searchFacilities(any(FacilitySearchDTO.class))).thenReturn(facilitySearchResults);

        mockMvc.perform(get("/api/bookings/facilities/search")
                .param("date", LocalDate.now().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].facilityId").value(1))
                .andExpect(jsonPath("$[0].resourceName").value("Test Facility"))
                .andExpect(jsonPath("$[0].location").value("Test Location"));
    }

    @Test
    @DisplayName("Search facilities with all parameters should filter correctly")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void searchFacilitiesWithAllParamsShouldFilterCorrectly() throws Exception {
        when(bookingService.searchFacilities(any(FacilitySearchDTO.class))).thenReturn(facilitySearchResults);

        mockMvc.perform(get("/api/bookings/facilities/search")
                .param("facilityId", "1")
                .param("resourceTypeId", "1")
                .param("resourceName", "Test Facility")
                .param("location", "Test Location")
                .param("capacity", "10")
                .param("date", LocalDate.now().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].facilityId").value(1));

        // Verify the service was called with appropriate filters
        verify(bookingService, times(1)).searchFacilities(any(FacilitySearchDTO.class));
    }

    @Test
    @DisplayName("Get dropdown options should return options")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void getDropdownOptionsShouldReturnOptions() throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put("resourceTypes", Arrays.asList());
        options.put("locations", Arrays.asList("Test Location"));
        options.put("resourceNames", Arrays.asList("Test Facility"));

        when(bookingService.getDropdownOptions()).thenReturn(options);

        mockMvc.perform(get("/api/bookings/dropdown-options")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locations").isArray())
                .andExpect(jsonPath("$.resourceNames").isArray())
                .andExpect(jsonPath("$.resourceTypes").isArray());
    }

    @Test
    @DisplayName("Create booking should return booking response")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void createBookingShouldReturnBookingResponse() throws Exception {
        when(bookingService.createBooking(any(BookingDTO.class))).thenReturn(bookingResponseDTO);
        // Mock behavior of factories
        when(emailServiceFactory.getEmailService(anyString())).thenReturn(emailService);
        when(emailContentStrategyFactory.getStrategy(anyString())).thenReturn(emailContentStrategy);

        // Mock behavior of the returned strategy and service
        when(emailContentStrategy.buildSubject(anyMap())).thenReturn("Test Subject");
        when(emailContentStrategy.buildBody(anyMap())).thenReturn("Test Body");
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.facilityName").value("Test Facility"))
                .andExpect(jsonPath("$.timeslot").value("14:00 - 15:00"));

        verify(emailServiceFactory, times(1)).getEmailService(anyString());
        verify(emailContentStrategyFactory, times(1)).getStrategy(anyString());
        verify(emailContentStrategy, times(1)).buildSubject(anyMap());
        verify(emailContentStrategy, times(1)).buildBody(anyMap());
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Create booking should handle service exceptions")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void createBookingShouldHandleServiceExceptions() throws Exception {
        when(bookingService.createBooking(any(BookingDTO.class)))
                .thenThrow(new RuntimeException("Insufficient credits"));

        try {
            mockMvc.perform(post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookingDTO)))
                    .andExpect(status().isInternalServerError());
        } catch (Exception e) {
            // This is expected because of how the controller handles the exception
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("Insufficient credits", e.getCause().getMessage());
        }

        verify(emailServiceFactory, times(0)).getEmailService(anyString());
        verify(emailContentStrategyFactory, times(0)).getStrategy(anyString());
    }

    @Test
    @DisplayName("Get upcoming approved bookings should return bookings")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void getUpcomingApprovedBookingsShouldReturnBookings() throws Exception {
        when(bookingService.getUpcomingApprovedOrConfirmedBookings(anyLong())).thenReturn(bookingList);

        mockMvc.perform(get("/api/bookings/upcoming-approved")
                .param("accountId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(1))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].facilityName").value("Test Facility"));
    }

    @Test
    @DisplayName("Get upcoming approved bookings should handle empty list")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void getUpcomingApprovedBookingsShouldHandleEmptyList() throws Exception {
        when(bookingService.getUpcomingApprovedOrConfirmedBookings(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/bookings/upcoming-approved")
                .param("accountId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Get pending future bookings should return bookings")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void getPendingFutureBookingsShouldReturnBookings() throws Exception {
        when(bookingService.getPendingFutureBookings(anyLong())).thenReturn(bookingList);

        mockMvc.perform(get("/api/bookings/pending-future")
                .param("accountId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(1))
                .andExpect(jsonPath("$[0].facilityName").value("Test Facility"));
    }

    @Test
    @DisplayName("Get booking history should return bookings")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void getBookingHistoryShouldReturnBookings() throws Exception {
        when(bookingService.getBookingHistory(anyLong(), anyString())).thenReturn(bookingList);

        mockMvc.perform(post("/api/bookings/history")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(1))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    @DisplayName("Get booking history with null status should work correctly")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void getBookingHistoryWithNullStatusShouldWorkCorrectly() throws Exception {
        bookingRequestDTO.setStatus(null);
        when(bookingService.getBookingHistory(anyLong(), eq(null))).thenReturn(bookingList);

        mockMvc.perform(post("/api/bookings/history")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].bookingId").value(1));

        verify(bookingService, times(1)).getBookingHistory(anyLong(), eq(null));
    }

    @Test
    @DisplayName("Delete booking should return success")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void deleteBookingShouldReturnSuccess() throws Exception {
        when(bookingService.deleteBooking(anyLong())).thenReturn(true);
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        EmailContentStrategy mockStrategy = mock(EmailContentStrategy.class);
        when(mockStrategy.buildSubject(anyMap())).thenReturn("Subject");
        when(mockStrategy.buildBody(anyMap())).thenReturn("Body");

        when(emailContentStrategyFactory.getStrategy("CANCELLED")).thenReturn(mockStrategy);

        EmailService mockEmailService = mock(EmailService.class);
        when(emailServiceFactory.getEmailService("customEmailService")).thenReturn(mockEmailService);

        mockMvc.perform(delete("/api/bookings/cancel-booking/1")
                .param("toEmail", "test@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("successfully")));
    }

    @Test
    @DisplayName("Delete booking should handle not found")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void deleteBookingShouldHandleNotFound() throws Exception {
        when(bookingService.deleteBooking(anyLong())).thenReturn(false);

        mockMvc.perform(delete("/api/bookings/cancel-booking/1")
                .param("toEmail", "test@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Delete booking should handle email failure")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void deleteBookingShouldHandleEmailFailure() throws Exception {
        when(bookingService.deleteBooking(anyLong())).thenReturn(true);
        when(emailContentStrategyFactory.getStrategy("CANCELLED")).thenReturn(emailContentStrategy);
        when(emailContentStrategy.buildSubject(anyMap())).thenReturn("Cancel Booking");
        when(emailContentStrategy.buildBody(anyMap())).thenReturn("Your booking has been cancelled.");

        when(emailServiceFactory.getEmailService("customEmailService")).thenReturn(emailService);
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(false);

        mockMvc.perform(delete("/api/bookings/cancel-booking/1")
                .param("toEmail", "test@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("but failed to send")));
    }

    @Test
    @DisplayName("Delete booking should handle messaging exception")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void deleteBookingShouldHandleMessagingException() throws Exception {
        when(bookingService.deleteBooking(anyLong())).thenReturn(true);
        when(emailContentStrategyFactory.getStrategy("CANCELLED")).thenReturn(emailContentStrategy);
        when(emailContentStrategy.buildSubject(anyMap())).thenReturn("Cancel Booking");
        when(emailContentStrategy.buildBody(anyMap())).thenReturn("Your booking has been cancelled.");

        when(emailServiceFactory.getEmailService("customEmailService")).thenReturn(emailService);

        doThrow(new MessagingException("Email error"))
                .when(emailService)
                .sendEmail(anyString(), anyString(), anyString());

        try {
            mockMvc.perform(delete("/api/bookings/cancel-booking/1")
                    .param("toEmail", "test@example.com")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());
        } catch (Exception e) {
            // This is expected because of how the controller handles the exception
            assertTrue(e.getCause() instanceof MessagingException);
            assertEquals("Email error", e.getCause().getMessage());
        }
    }

    @Test
    @DisplayName("Update booking status to APPROVED should return success")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void updateBookingStatusToApprovedShouldReturnSuccess() throws Exception {
        when(bookingService.updateBookingStatus(anyLong(), any(BookingStatus.class))).thenReturn(true);
        when(emailContentStrategyFactory.getStrategy("ADMINPPROVED")).thenReturn(emailContentStrategy);
        when(emailContentStrategy.buildSubject(anyMap())).thenReturn("Booking Approved");
        when(emailContentStrategy.buildBody(anyMap())).thenReturn("Your booking has been approved.");

        when(emailServiceFactory.getEmailService("customEmailService")).thenReturn(emailService);
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        mockMvc.perform(put("/api/bookings/update/1")
                .param("toEmail", "test@example.com")
                .param("status", "APPROVED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("approved successfully")));
    }

    @Test
    @DisplayName("Update booking status to REJECTED should handle rejection reason")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void updateBookingStatusToRejectedShouldHandleRejectionReason() throws Exception {
        when(bookingService.updateBookingStatus(anyLong(), any(BookingStatus.class))).thenReturn(true);
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        EmailContentStrategy mockStrategy = mock(EmailContentStrategy.class);
        when(mockStrategy.buildSubject(anyMap())).thenReturn("Subject");
        when(mockStrategy.buildBody(anyMap())).thenReturn("Body");

        when(emailContentStrategyFactory.getStrategy("REJECTED")).thenReturn(mockStrategy);

        EmailService mockEmailService = mock(EmailService.class);
        when(emailServiceFactory.getEmailService("customEmailService")).thenReturn(mockEmailService);

        mockMvc.perform(put("/api/bookings/update/1")
                .param("toEmail", "test@example.com")
                .param("status", "REJECTED")
                .param("rejectReason", "Facility unavailable")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("rejected successfully")));
    }

    @Test
    @DisplayName("Update booking status should handle booking not found")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void updateBookingStatusShouldHandleBookingNotFound() throws Exception {
        when(bookingService.updateBookingStatus(anyLong(), any(BookingStatus.class))).thenReturn(false);

        mockMvc.perform(put("/api/bookings/update/1")
                .param("toEmail", "test@example.com")
                .param("status", "APPROVED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("not found")));
    }

    @Test
    @DisplayName("Get pending bookings should return bookings list")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void getPendingBookingsShouldReturnBookingsList() throws Exception {
        when(bookingService.getBookingsByStatus(anyString())).thenReturn(bookingList);

        mockMvc.perform(get("/api/bookings/pending-bookings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(1))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    @DisplayName("Get pending bookings should handle empty list")
    @WithMockUser(username = "test@example.com", roles = { "STUDENT" })
    void getPendingBookingsShouldHandleEmptyList() throws Exception {
        when(bookingService.getBookingsByStatus(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/bookings/pending-bookings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}