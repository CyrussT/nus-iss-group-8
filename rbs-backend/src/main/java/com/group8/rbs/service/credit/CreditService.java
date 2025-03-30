package com.group8.rbs.service.credit;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.group8.rbs.repository.AccountRepository;
import com.group8.rbs.repository.CreditRepository;

@Service
public class CreditService {
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
}
