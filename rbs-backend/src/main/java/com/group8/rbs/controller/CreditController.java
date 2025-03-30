package com.group8.rbs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.group8.rbs.service.credit.CreditService;

@RestController
@RequestMapping("/api/credit")
public class CreditController {
    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping()
    public ResponseEntity<Double> getCredit(
        @RequestParam(required = true) String email
    ) {
        return ResponseEntity.ok(creditService.getCredit(email));
    }
}
