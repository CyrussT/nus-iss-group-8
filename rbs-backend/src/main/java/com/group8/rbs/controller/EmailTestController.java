package com.group8.rbs.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.group8.rbs.util.EmailWrapper;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailTestController {
    private final EmailWrapper emailWrapper;

    @GetMapping("/test")
    public ResponseEntity<?> testEmail() {
        return ResponseEntity.ok(emailWrapper.sendEmail("tong.cyrus@gmail.com", "TEST", "<h1>test</h1>"));
    }

}
