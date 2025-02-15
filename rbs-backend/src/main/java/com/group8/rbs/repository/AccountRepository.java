package com.group8.rbs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group8.rbs.entities.Account;
import com.group8.rbs.enums.AccountType;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email);
    Optional<Account> findByEmailAndPassword(String email, String password);
    Optional<Account> findByEmailAndAccountType(String email, AccountType accountType);
    Optional<Account> findByEmailAndAccountTypeAndPassword(String email, AccountType accountType, String password);
}
