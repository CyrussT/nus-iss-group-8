package com.group8.rbs.service.credit;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group8.rbs.repository.AccountRepository;
import com.group8.rbs.repository.CreditRepository;

@Service
public class CreditService {
    private static final Logger logger = LoggerFactory.getLogger(CreditService.class);
    public final CreditRepository creditRepository;
    public final AccountRepository accountRepository;

    public CreditService(CreditRepository creditRepository, AccountRepository accountRepository) {
        this.creditRepository = creditRepository;
        this.accountRepository = accountRepository;
    }

    public Double getCredit(String email) {
        Optional<Integer> accountId = accountRepository.findAccountIdByEmail(email);
        if (!accountId.isPresent()) {
            throw new RuntimeException("Account not found");
        }
        return creditRepository.findCreditBalanceByAccountId((long) accountId.get());
    }
    
    /**
     * Add credits to an account (for refunds)
     * 
     * @param accountId The account ID to add credits to
     * @param amount The amount of credits to add
     * @return true if successful, false otherwise
     */
    @Transactional
    public boolean addCredits(Long accountId, Double amount) {
        if (accountId == null || amount == null || amount <= 0) {
            logger.warn("Invalid parameters for adding credits: accountId={}, amount={}", accountId, amount);
            return false;
        }
        
        try {
            int updated = creditRepository.addCredits(accountId, amount);
            if (updated > 0) {
                logger.info("Successfully added {} credits to account ID: {}", amount, accountId);
                return true;
            } else {
                logger.warn("No account found for ID: {} when adding credits", accountId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error adding credits to account ID: {}, amount: {}", accountId, amount, e);
            return false;
        }
    }
}