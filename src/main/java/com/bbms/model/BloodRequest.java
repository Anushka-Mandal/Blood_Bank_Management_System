package com.bbms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BloodRequest - Association between Recipient and BloodUnit.
 * From class diagram: Recipient → BloodRequest (Association).
 * GRASP: Low Coupling — BloodRequest holds only request-level data.
 */
@Entity
@Table(name = "blood_requests")
@Getter @Setter @NoArgsConstructor
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @NotBlank(message = "Blood group is required")
    @Column(nullable = false)
    private String bloodGroup;

    @Min(value = 1, message = "Quantity must be at least 1 unit")
    @Max(value = 10, message = "Cannot request more than 10 units at once")
    @Column(nullable = false)
    private int quantity;

    @NotNull(message = "Required date is required")
    @Column(nullable = false)
    private LocalDate requiredDate;

    @Column(nullable = false)
    private String urgency = "NORMAL"; // NORMAL, URGENT, CRITICAL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    private String adminNotes;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private Recipient recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_blood_unit_id")
    private BloodUnit allocatedBloodUnit;

    public enum RequestStatus {
        PENDING,        // Just submitted
        FORWARDED,      // Maintainer forwarded to Admin
        APPROVED,       // Admin approved
        ALLOCATED,      // Blood unit assigned
        FULFILLED,      // Blood delivered
        REJECTED        // Rejected
    }

    // ===== Business Methods =====
    public void updateStatus(RequestStatus status, String notes) {
        this.status = status;
        this.adminNotes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isUrgent() {
        return "URGENT".equals(urgency) || "CRITICAL".equals(urgency);
    }

    public boolean canBeAllocated() {
        return this.status == RequestStatus.APPROVED;
    }
}
