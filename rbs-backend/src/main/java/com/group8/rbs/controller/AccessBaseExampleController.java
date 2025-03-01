package com.group8.rbs.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.group8.rbs.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class AccessBaseExampleController {
    
    // Example endpoint for a student
    @GetMapping("student")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<String> accessStudent() {
        return ResponseEntity.ok("Student Access " + SecurityUtils.getCurrentUserEmail());
    }

    // Example endpoint for an admin 
    @GetMapping("admin")
    @PreAuthorize("hasRole('ROLE_ADMINISTRATOR')")
    public ResponseEntity<String> accessAdmin() {
        return ResponseEntity.ok("Admin Access " + SecurityUtils.getCurrentUserEmail());
    }
}
