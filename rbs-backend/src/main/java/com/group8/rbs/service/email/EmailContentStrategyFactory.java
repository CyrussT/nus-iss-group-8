package com.group8.rbs.service.email;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Admin;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EmailContentStrategyFactory {

    private final Map<String, EmailContentStrategy> strategyMap = new HashMap<>();

    public EmailContentStrategyFactory() {
        strategyMap.put("PENDING", new PendingApprovalEmailContent());
        strategyMap.put("SYSTEMAPPROVED", new SystemApprovedEmailContent());
        strategyMap.put("ADMINPPROVED", new AdminApprovedEmailContent());
        strategyMap.put("REJECTED", new RejectionEmailContent());
        strategyMap.put("CANCELLED", new CancellationEmailContent());
    }

    public EmailContentStrategy getStrategy(String status) {
        EmailContentStrategy strategy = strategyMap.get(status.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for status: " + status);
        }
        return strategy;
    }
}
