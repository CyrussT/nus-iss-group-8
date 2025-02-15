package com.group8.rbs.service.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.group8.rbs.dto.auth.AuthResponse;
import com.group8.rbs.dto.auth.LoginRequest;
import com.group8.rbs.dto.auth.RegisterRequest;
import com.group8.rbs.entities.Account;
import com.group8.rbs.entities.Credit;
import com.group8.rbs.enums.AccountType;
import com.group8.rbs.exception.AuthException;
import com.group8.rbs.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

// src/main/java/com/group8/rbs/service/AuthService.java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthException("Email already exists");
        }

        String salt = BCrypt.gensalt();
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        var account = Account.builder()
                .email(request.getEmail())
                .password(hashedPassword)
                .salt(salt)
                .accountType(request.getAccountType())
                .build();

        // Initialize credit for new student accounts
        if (request.getAccountType() == AccountType.STUDENT) {
            var credit = Credit.builder()
                    .account(account)
                    .creditBalance(40.0) // 4 hours initial credit
                    .build();
            account.setCredit(credit);
        }

        accountRepository.save(account);
        
        String token = jwtService.generateToken(account);
        return AuthResponse.builder()
                .token(token)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw new AuthException("Invalid credentials");
        }

        var account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("User not found"));
        
        String token = jwtService.generateToken(account);
        return AuthResponse.builder()
                .token(token)
                .build();
    }
}