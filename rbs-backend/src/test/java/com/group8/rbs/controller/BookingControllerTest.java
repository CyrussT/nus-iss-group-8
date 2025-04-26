package com.group8.rbs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.group8.rbs.dto.booking.BookingDTO;
import com.group8.rbs.dto.booking.BookingResponseDTO;
import com.group8.rbs.dto.booking.FacilitySearchDTO;
import com.group8.rbs.enums.BookingStatus;
import com.group8.rbs.security.CustomUserDetailsService;
import com.group8.rbs.service.security.JwtService;
import com.group8.rbs.service.security.AuthService;
import com.group8.rbs.security.SecurityConfig;
import com.group8.rbs.service.booking.BookingService;
import com.group8.rbs.service.email.CustomEmailService;

@WebMvcTest(BookingController.class)
@Import({SecurityConfig.class, TestConfig.class})
@ActiveProfiles("test")
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;
    
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

    private BookingDTO bookingDTO;
    private BookingResponseDTO bookingResponseDTO;
    private FacilitySearchDTO facilitySearchDTO;
    private List<FacilitySearchDTO> facilitySearchResults;
    private List<BookingResponseDTO> bookingList;

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
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"STUDENT"})
    void searchFacilitiesShouldReturnFacilityList() throws Exception {
        when(bookingService.searchFacilities(any(FacilitySearchDTO.class))).thenReturn(facilitySearchResults);

        mockMvc.perform(get("/api/bookings/facilities/search")
                .param("date", LocalDate.now().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].facilityId").value(1))
                .andExpect(jsonPath("$[0].resourceName").value("Test Facility"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"STUDENT"})
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
                .andExpect(jsonPath("$.resourceNames").isArray());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"STUDENT"})
    void createBookingShouldReturnBookingResponse() throws Exception {
        when(bookingService.createBooking(any(BookingDTO.class))).thenReturn(bookingResponseDTO);
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"STUDENT"})
    void getUpcomingApprovedBookingsShouldReturnBookings() throws Exception {
        when(bookingService.getUpcomingApprovedOrConfirmedBookings(anyLong())).thenReturn(bookingList);

        mockMvc.perform(get("/api/bookings/upcoming-approved")
                .param("accountId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(1))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"STUDENT"})
    void getPendingFutureBookingsShouldReturnBookings() throws Exception {
        when(bookingService.getPendingFutureBookings(anyLong())).thenReturn(bookingList);

        mockMvc.perform(get("/api/bookings/pending-future")
                .param("accountId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(1));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"STUDENT"})
    void getBookingHistoryShouldReturnBookings() throws Exception {
        when(bookingService.getBookingHistory(anyLong(), anyString())).thenReturn(bookingList);

        mockMvc.perform(post("/api/bookings/history")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\": 1, \"status\": \"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(1));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"STUDENT"})
    void deleteBookingShouldReturnSuccess() throws Exception {
        when(bookingService.deleteBooking(anyLong())).thenReturn(true);
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        mockMvc.perform(delete("/api/bookings/cancel-booking/1")
                .param("toEmail", "test@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("successfully")));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"STUDENT"})
    void updateBookingStatusShouldReturnSuccess() throws Exception {
        when(bookingService.updateBookingStatus(anyLong(), any(BookingStatus.class))).thenReturn(true);
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        mockMvc.perform(put("/api/bookings/update/1")
                .param("toEmail", "test@example.com")
                .param("status", "APPROVED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("approved successfully")));
    }
    
    @Test
    @WithMockUser(username = "test@example.com", roles = {"STUDENT"})
    void getPendingBookingsShouldReturnBookingsList() throws Exception {
        when(bookingService.getBookingsByStatus(anyString())).thenReturn(bookingList);
        
        mockMvc.perform(get("/api/bookings/pending-bookings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(1));
    }
}