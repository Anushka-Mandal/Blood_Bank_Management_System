package com.bbms.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipient - Extends User (Inheritance relationship from class diagram).
 * GRASP: Creator — Recipient creates BloodRequests.
 * OO Principle: High Cohesion — only blood request-related behavior.
 */
@Entity
@Table(name = "recipients")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter @Setter @NoArgsConstructor
public class Recipient extends User {

    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BloodRequest> bloodRequests = new ArrayList<>();

    // ===== Business Method: Use Case "Request Blood" =====
    public BloodRequest requestBlood(String bloodGroup, int quantity, java.time.LocalDate requiredDate, String urgency) {
        BloodRequest request = new BloodRequest();
        request.setBloodGroup(bloodGroup);
        request.setQuantity(quantity);
        request.setRequiredDate(requiredDate);
        request.setUrgency(urgency);
        request.setRecipient(this);
        request.setStatus(BloodRequest.RequestStatus.PENDING);
        this.bloodRequests.add(request);
        return request;
    }

    public long getPendingRequestCount() {
        return bloodRequests.stream()
                .filter(r -> r.getStatus() == BloodRequest.RequestStatus.PENDING)
                .count();
    }
}
