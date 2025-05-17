package com.group8.rbs.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.group8.rbs.config.TestConfig;
import com.group8.rbs.security.SecurityConfig;
import com.group8.rbs.service.credit.CreditService;

@WebMvcTest(CreditController.class)
@Import({SecurityConfig.class, TestConfig.class})
@ActiveProfiles("test")
public class CreditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreditService creditService;

    @Test
    @DisplayName("Get credit should return credit balance")
    @WithMockUser
    void getCreditShouldReturnCreditBalance() throws Exception {
        // Set up mock service response
        when(creditService.getCredit(anyString())).thenReturn(120.0);

        // Perform request and verify response
        mockMvc.perform(get("/api/credit")
                .param("email", "test@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("120.0"));
        
        // Verify service method was called with correct parameters
        verify(creditService, times(1)).getCredit("test@example.com");
    }

    @Test
    @DisplayName("Get credit should require email parameter")
    @WithMockUser
    void getCreditShouldRequireEmailParameter() throws Exception {
        // Perform request without required parameter
        mockMvc.perform(get("/api/credit")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}