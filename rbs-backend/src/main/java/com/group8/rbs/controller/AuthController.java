package com.group8.rbs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.group8.rbs.dto.auth.AuthResponse;
import com.group8.rbs.dto.auth.LoginRequest;
import com.group8.rbs.dto.auth.RegisterRequest;
import com.group8.rbs.service.security.AuthService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/student-id")
    public ResponseEntity<String> getStudentId(@RequestParam String email) {
        return ResponseEntity.ok(authService.getStudentIdByEmail(email));
    }

    @GetMapping("/account-id")
    public ResponseEntity<Integer> getAccountId(@RequestParam String email) {
        return ResponseEntity.ok(authService.getAccountIdByEmail(email));
    }

}
