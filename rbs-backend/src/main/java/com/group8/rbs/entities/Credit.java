package com.group8.rbs.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "TBL_CREDIT")
@Getter
@Setter
public class Credit {
    @Id
    @Column(name = "ACCOUNT_ID")
    private Long accountId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "ACCOUNT_ID")
    private Account account;

    @Column(name = "CREDIT_BALANCE", nullable = false)
    private Double creditBalance;
}