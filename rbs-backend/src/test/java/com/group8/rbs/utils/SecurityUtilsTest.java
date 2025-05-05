package com.group8.rbs.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.group8.rbs.util.SecurityUtils;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserEmail_shouldReturnEmail_whenAuthenticated() {
        UserDetails userDetails = new User("test@example.com", "password", new java.util.ArrayList<>());
        UsernamePasswordAuthenticationToken auth = 
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        String email = SecurityUtils.getCurrentUserEmail();
        assertEquals("test@example.com", email);
    }

    @Test
    void getCurrentUserEmail_shouldReturnNull_whenNotAuthenticated() {
        // No authentication set
        assertNull(SecurityUtils.getCurrentUserEmail());
    }

    @Test
    void getCurrentUserEmail_shouldReturnNull_whenPrincipalNotUserDetails() {
        UsernamePasswordAuthenticationToken auth = 
            new UsernamePasswordAuthenticationToken("anonymousUser", null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertNull(SecurityUtils.getCurrentUserEmail());
    }
}
