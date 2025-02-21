package com.group8.rbs.service.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.group8.rbs.dto.auth.AuthResponse;
import com.group8.rbs.dto.auth.LoginRequest;
import com.group8.rbs.dto.auth.RegisterRequest;
import com.group8.rbs.entities.Account;
import com.group8.rbs.enums.AccountType;
import com.group8.rbs.exception.AuthException;
import com.group8.rbs.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Account account;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setAccountType(AccountType.STUDENT);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        account = Account.builder()
                .email("test@example.com")
                .password("hashedPassword")
                .accountType(AccountType.STUDENT)
                .build();
    }

    @Test
    void registerShouldCreateNewAccount() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(jwtService.generateToken(any(Account.class))).thenReturn("dummy.jwt.token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerShouldThrowExceptionWhenEmailExists() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));

        assertThrows(AuthException.class, () -> authService.register(registerRequest));
    }

    @Test
    void loginShouldReturnToken() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(jwtService.generateToken(any(Account.class))).thenReturn("dummy.jwt.token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginShouldThrowExceptionWhenUserNotFound() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(AuthException.class, () -> authService.login(loginRequest));
    }
}
