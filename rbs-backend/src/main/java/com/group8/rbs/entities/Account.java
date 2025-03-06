package com.group8.rbs.entities;

import java.util.List;

import com.group8.rbs.enums.AccountType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TBL_ACCOUNT")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ACCOUNT_ID")
    private Long accountId;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "STUDENT_ID", nullable = true, unique = true)
    private String studentId;

    @Column(name = "EMAIL", nullable = true, unique = true)
    private String email;

    @Column(name = "ENROLLMENT_YEAR", nullable = true)
    private Integer enrollmentYear;

    @Column(name = "STATUS", nullable = false)
    private String status;  // Added Status (e.g., Active, Inactive, Graduated)

    @Column(name = "PASSWORD", nullable = false)
    private String password;

    @Column(name = "SALT")
    private String salt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACCOUNT_TYPE", nullable = false)
    private AccountType accountType;

    @OneToMany(mappedBy = "account")
    private List<Booking> bookings;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
    private Credit credit;
}
