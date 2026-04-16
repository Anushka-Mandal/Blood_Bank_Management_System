package com.bbms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * User - Base entity implementing role-based access control.
 * Uses Single Table Inheritance (STI) — Donor and Recipient extend this.
 * GRASP: Information Expert — User owns its own authentication data.
 * Design Pattern: Template Method — subclasses define role-specific behavior.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter @Setter @NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @Min(value = 18, message = "Must be at least 18 years old")
    @Max(value = 65, message = "Must be at most 65 years old")
    @Column(nullable = false)
    private int age;

    @NotBlank(message = "NID is required")
    @Column(nullable = false, unique = true)
    private String nid;

    @NotBlank(message = "Blood group is required")
    @Column(nullable = false)
    private String bloodGroup;

    @NotBlank(message = "Username is required")
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // stored as BCrypt hash

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus = AccountStatus.PENDING;

    // ===== Enums =====
    public enum Role {
        DONOR, RECIPIENT, ADMIN, MAINTAINER, HEMATOLOGY
    }

    public enum AccountStatus {
        PENDING, APPROVED, REJECTED
    }

    // ===== Constructor for registration =====
    public User(String name, int age, String nid, String bloodGroup,
                String username, String password, Role role) {
        this.name = name;
        this.age = age;
        this.nid = nid;
        this.bloodGroup = bloodGroup;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // ===== Business logic =====
    public boolean isApproved() {
        return this.accountStatus == AccountStatus.APPROVED;
    }

    public boolean isPending() {
        return this.accountStatus == AccountStatus.PENDING;
    }
}
