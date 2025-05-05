package com.group8.rbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.group8.rbs.entities.Credit;

@Repository
public interface CreditRepository extends JpaRepository<Credit, Long> {

    // Retrieve available credits for a given account
    @Query("SELECT c.creditBalance FROM Credit c WHERE c.accountId = :accountId")
    Double findCreditBalanceByAccountId(Long accountId);

    // Combined check and deduct in one operation
    @Modifying
    @Query("UPDATE Credit c SET c.creditBalance = c.creditBalance - :amount " +
           "WHERE c.accountId = :accountId AND c.creditBalance >= :amount")
    int checkAndDeductCredits(@Param("accountId") Long accountId, @Param("amount") Double amount);
    
    // Add credits back to an account (for refunds)
    @Modifying
    @Transactional
    @Query("UPDATE Credit c SET c.creditBalance = c.creditBalance + :amount " +
           "WHERE c.accountId = :accountId")
    int addCredits(@Param("accountId") Long accountId, @Param("amount") Integer amount);
}