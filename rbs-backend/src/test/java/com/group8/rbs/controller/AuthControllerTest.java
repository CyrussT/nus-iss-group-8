package com.group8.rbs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group8.rbs.config.TestConfig;
import com.group8.rbs.dto.auth.AuthResponse;
import com.group8.rbs.dto.auth.LoginRequest;
import com.group8.rbs.dto.auth.RegisterRequest;
import com.group8.rbs.enums.AccountType;
import com.group8.rbs.exception.AuthException;
import com.group8.rbs.security.SecurityConfig;
import com.group8.rbs.service.security.AuthService;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, TestConfig.class})
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAccountType(AccountType.STUDENT);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        authResponse = AuthResponse.builder()
                .token("dummy.jwt.token")
                .build();
    }

    @Test
    @DisplayName("Register should return token")
    @WithMockUser
    void registerShouldReturnToken() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dummy.jwt.token"));
        
        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Register should handle email already exists")
    @WithMockUser
    void registerShouldHandleEmailAlreadyExists() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AuthException("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @DisplayName("Register should handle invalid request body")
    @WithMockUser
    void registerShouldHandleInvalidRequestBody() throws Exception {
        String invalidJson = "{\"email\": \"test@example.com\", invalid}";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Login should return token")
    @WithMockUser  
    void loginShouldReturnToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dummy.jwt.token"));
        
        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Login should handle invalid credentials")
    @WithMockUser
    void loginShouldHandleInvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @DisplayName("Login should handle user not found")
    @WithMockUser
    void loginShouldHandleUserNotFound() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthException("User not found"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("Login should handle invalid request body")
    @WithMockUser
    void loginShouldHandleInvalidRequestBody() throws Exception {
        String invalidJson = "{\"email\": \"test@example.com\", invalid}";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Get student ID should return student ID")
    @WithMockUser
    void getStudentIdShouldReturnStudentId() throws Exception {
        when(authService.getStudentIdByEmail(anyString())).thenReturn("S12345");

        mockMvc.perform(get("/api/auth/student-id")
                .param("email", "test@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("S12345"));
        
        verify(authService, times(1)).getStudentIdByEmail("test@example.com");
    }

    @Test
    @DisplayName("Get student ID should handle student ID not found")
    @WithMockUser
    void getStudentIdShouldHandleStudentIdNotFound() throws Exception {
        when(authService.getStudentIdByEmail(anyString()))
                .thenThrow(new AuthException("Student ID not found for email: test@example.com"));

        mockMvc.perform(get("/api/auth/student-id")
                .param("email", "test@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Student ID not found for email: test@example.com"));
    }

    @Test
    @DisplayName("Get student ID should require email parameter")
    @WithMockUser
    void getStudentIdShouldRequireEmailParameter() throws Exception {
        mockMvc.perform(get("/api/auth/student-id")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Get account ID should return account ID")
    @WithMockUser
    void getAccountIdShouldReturnAccountId() throws Exception {
        when(authService.getAccountIdByEmail(anyString())).thenReturn(123);

        mockMvc.perform(get("/api/auth/account-id")
                .param("email", "test@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("123"));
        
        verify(authService, times(1)).getAccountIdByEmail("test@example.com");
    }

    @Test
    @DisplayName("Get account ID should handle account ID not found")
    @WithMockUser
    void getAccountIdShouldHandleAccountIdNotFound() throws Exception {
        when(authService.getAccountIdByEmail(anyString()))
                .thenThrow(new AuthException("Account ID not found for email: test@example.com"));

        mockMvc.perform(get("/api/auth/account-id")
                .param("email", "test@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account ID not found for email: test@example.com"));
    }

    @Test
    @DisplayName("Get account ID should require email parameter")
    @WithMockUser
    void getAccountIdShouldRequireEmailParameter() throws Exception {
        mockMvc.perform(get("/api/auth/account-id")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}