package com.bbms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BloodUnit - Represents an individual unit of donated blood.
 * From class diagram: BloodUnit → TestResult (Composition).
 * GRASP: Information Expert — knows its own test status and availability.
 * Design Pattern: Builder used in BloodService to construct safely.
 */
@Entity
@Table(name = "blood_units")
@Getter @Setter @NoArgsConstructor
public class BloodUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bloodId;

    @Column(nullable = false)
    private String bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestStatus testStatus = TestStatus.PENDING;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private LocalDateTime collectedAt = LocalDateTime.now();

    private boolean available = false; // only true after test passes

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id")
    private Donor donor;

    // Composition: TestResult is part of BloodUnit
    @Embedded
    private TestResult testResult;

    public enum TestStatus {
        PENDING,    // awaiting hematology test
        SAFE,       // passed all tests
        UNSAFE,     // failed tests, discard
        EXPIRED     // past expiry date
    }

    // ===== Business Methods =====
    public void markSafe(String testedBy, String notes) {
        this.testStatus = TestStatus.SAFE;
        this.available = true;
        this.testResult = new TestResult(testedBy, true, notes, LocalDateTime.now());
    }

    public void markUnsafe(String testedBy, String notes) {
        this.testStatus = TestStatus.UNSAFE;
        this.available = false;
        this.testResult = new TestResult(testedBy, false, notes, LocalDateTime.now());
    }

    public boolean isEligibleForAllocation() {
        return this.available &&
               this.testStatus == TestStatus.SAFE &&
               !LocalDate.now().isAfter(this.expiryDate);
    }

    public void allocate() {
        this.available = false;
    }
}
