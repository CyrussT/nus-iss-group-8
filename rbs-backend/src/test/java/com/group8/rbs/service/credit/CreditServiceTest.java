package com.group8.rbs.service.credit;

import com.group8.rbs.repository.AccountRepository;
import com.group8.rbs.repository.CreditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreditServiceTest {

    
    private CreditRepository creditRepository;
    private AccountRepository accountRepository;
    private CreditService creditService;

    @BeforeEach
    void setUp() {
        creditRepository = mock(CreditRepository.class);
        accountRepository = mock(AccountRepository.class);
        creditService = new CreditService(creditRepository, accountRepository);
    }

    @Test
    void testGetCredit_successful() {
        String email = "test@example.com";
        int accountId = 42;
        double creditBalance = 100.0;

        when(accountRepository.findAccountIdByEmail(email)).thenReturn(Optional.of(accountId));
        when(creditRepository.findCreditBalanceByAccountId((long) accountId)).thenReturn(creditBalance);

        double result = creditService.getCredit(email);

        assertEquals(creditBalance, result);
        verify(accountRepository).findAccountIdByEmail(email);
        verify(creditRepository).findCreditBalanceByAccountId((long) accountId);
    }

    @Test
    void testGetCredit_accountNotFound() {
        String email = "nonexistent@example.com";
        when(accountRepository.findAccountIdByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> creditService.getCredit(email));
        assertEquals("Account not found", exception.getMessage());

        verify(accountRepository).findAccountIdByEmail(email);
        verify(creditRepository, never()).findCreditBalanceByAccountId(anyLong());
    }

    @Test
    void testAddCredits_successful() {
        Long accountId = 42L;
        Integer amount = 50;

        when(creditRepository.addCredits(accountId, amount)).thenReturn(1);

        boolean result = creditService.addCredits(accountId, amount);

        assertTrue(result);
        verify(creditRepository).addCredits(accountId, amount);
    }

    @Test
    void testAddCredits_invalidAmount() {
        boolean result = creditService.addCredits(42L, -10);

        assertFalse(result);
        verify(creditRepository, never()).addCredits(anyLong(), anyInt());
    }

    @Test
    void testAddCredits_nullAccountId() {
        boolean result = creditService.addCredits(null, 10);

        assertFalse(result);
        verify(creditRepository, never()).addCredits(anyLong(), anyInt());
    }

    @Test
    void testAddCredits_nullAmount() {
        boolean result = creditService.addCredits(42L, null);

        assertFalse(result);
        verify(creditRepository, never()).addCredits(anyLong(), anyInt());
    }

    @Test
    void testAddCredits_noRowsUpdated() {
        Long accountId = 42L;
        Integer amount = 20;

        when(creditRepository.addCredits(accountId, amount)).thenReturn(0);

        boolean result = creditService.addCredits(accountId, amount);

        assertFalse(result);
    }

    @Test
    void testAddCredits_exceptionThrown() {
        Long accountId = 42L;
        Integer amount = 10;

        when(creditRepository.addCredits(accountId, amount)).thenThrow(new RuntimeException("DB error"));

        boolean result = creditService.addCredits(accountId, amount);

        assertFalse(result);
    }
}
