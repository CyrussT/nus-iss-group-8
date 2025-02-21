package com.group8.rbs.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.group8.rbs.util.EmailWrapper;

@TestConfiguration
public class TestConfig {
    
    @Bean
    public EmailWrapper emailWrapper() {
        return Mockito.mock(EmailWrapper.class);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return Mockito.mock(UserDetailsService.class);
    }
}
