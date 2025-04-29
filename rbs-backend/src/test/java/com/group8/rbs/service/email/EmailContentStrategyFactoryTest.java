package com.group8.rbs.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailContentStrategyFactoryTest {

    private EmailContentStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new EmailContentStrategyFactory();
    }

    @Test
    void testGetStrategy_returnsPendingApprovalStrategy() {
        EmailContentStrategy strategy = factory.getStrategy("PENDING");
        assertTrue(strategy instanceof PendingApprovalEmailContent);
    }

    @Test
    void testGetStrategy_returnsSystemApprovedStrategy() {
        EmailContentStrategy strategy = factory.getStrategy("SYSTEMAPPROVED");
        assertTrue(strategy instanceof SystemApprovedEmailContent);
    }

    @Test
    void testGetStrategy_returnsAdminApprovedStrategy() {
        EmailContentStrategy strategy = factory.getStrategy("ADMINPPROVED");
        assertTrue(strategy instanceof AdminApprovedEmailContent);
    }

    @Test
    void testGetStrategy_returnsRejectionStrategy() {
        EmailContentStrategy strategy = factory.getStrategy("REJECTED");
        assertTrue(strategy instanceof RejectionEmailContent);
    }

    @Test
    void testGetStrategy_returnsCancellationStrategy() {
        EmailContentStrategy strategy = factory.getStrategy("CANCELLED");
        assertTrue(strategy instanceof CancellationEmailContent);
    }

    @Test
    void testGetStrategy_isCaseInsensitive() {
        EmailContentStrategy strategy = factory.getStrategy("pending");
        assertTrue(strategy instanceof PendingApprovalEmailContent);
    }

    @Test
    void testGetStrategy_throwsExceptionForUnknownStatus() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            factory.getStrategy("UNKNOWN");
        });

        assertEquals("No strategy found for status: UNKNOWN", exception.getMessage());
    }
}
