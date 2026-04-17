package com.bbms.service;

import com.bbms.model.*;
import com.bbms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * BloodRequestService - Handles:
 *   UC4: Recipient Requests Blood
 *   UC5: Maintainer Forwards Request
 *   UC6: Admin Approves/Rejects Request
 *   UC7: Admin Allocates Blood
 *
 * GRASP: Controller — owns blood request lifecycle.
 * SOLID: Open-Closed — status transitions are isolated methods.
 * Design Pattern: Command — each status change is a discrete action.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BloodRequestService {

    private final BloodRequestRepository bloodRequestRepository;
    private final BloodUnitRepository bloodUnitRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    // ===== UC4: Recipient submits a blood request =====
    public BloodRequest submitRequest(Long recipientId, String bloodGroup,
                                      int quantity, LocalDate requiredDate, String urgency) {
        Recipient recipient = (Recipient) userRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        if (!recipient.isApproved()) {
            throw new RuntimeException("Your account must be approved before requesting blood.");
        }

        BloodRequest request = recipient.requestBlood(bloodGroup, quantity, requiredDate, urgency);
        return bloodRequestRepository.save(request);
    }

    // ===== UC5: Maintainer forwards request to Admin =====
    public BloodRequest forwardRequest(Long requestId, String notes) {
        BloodRequest request = findById(requestId);
        if (request.getStatus() != BloodRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Only PENDING requests can be forwarded.");
        }
        request.updateStatus(BloodRequest.RequestStatus.FORWARDED, notes);
        return bloodRequestRepository.save(request);
    }

    // ===== UC6: Admin approves a forwarded request =====
    public BloodRequest approveRequest(Long requestId, String notes) {
        BloodRequest request = findById(requestId);
        request.updateStatus(BloodRequest.RequestStatus.APPROVED, notes);
        // Reserve inventory
        inventoryRepository.findByBloodGroup(request.getBloodGroup())
                .ifPresent(inv -> inv.reserveUnits(request.getQuantity()));
        return bloodRequestRepository.save(request);
    }

    // ===== UC6: Admin rejects request =====
    public BloodRequest rejectRequest(Long requestId, String reason) {
        BloodRequest request = findById(requestId);
        request.updateStatus(BloodRequest.RequestStatus.REJECTED, reason);
        return bloodRequestRepository.save(request);
    }

    // ===== UC7: Admin allocates blood unit to request =====
    public BloodRequest allocateBlood(Long requestId, Long bloodUnitId) {
        BloodRequest request = findById(requestId);
        BloodUnit unit = bloodUnitRepository.findById(bloodUnitId)
                .orElseThrow(() -> new RuntimeException("Blood unit not found"));

        if (!unit.isEligibleForAllocation()) {
            throw new RuntimeException("This blood unit is not eligible for allocation.");
        }
        if (!unit.getBloodGroup().equals(request.getBloodGroup())) {
            throw new RuntimeException("Blood group mismatch.");
        }

        unit.allocate();
        request.setAllocatedBloodUnit(unit);
        request.updateStatus(BloodRequest.RequestStatus.ALLOCATED, "Blood unit allocated successfully.");

        // Update inventory
        inventoryRepository.findByBloodGroup(unit.getBloodGroup())
                .ifPresent(inv -> inv.fulfillReservation(1));

        bloodUnitRepository.save(unit);
        return bloodRequestRepository.save(request);
    }

    // ===== Mark as Fulfilled =====
    public BloodRequest fulfillRequest(Long requestId) {
        BloodRequest request = findById(requestId);
        request.updateStatus(BloodRequest.RequestStatus.FULFILLED, "Blood delivered to recipient.");
        return bloodRequestRepository.save(request);
    }

    // ===== Queries =====
    public List<BloodRequest> getAllRequests() {
        return bloodRequestRepository.findAll();
    }

    public List<BloodRequest> getRequestsByStatus(BloodRequest.RequestStatus status) {
        return bloodRequestRepository.findByStatus(status);
    }

    public List<BloodRequest> getRequestsByRecipient(Recipient recipient) {
        return bloodRequestRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    public List<BloodRequest> getUrgentRequests() {
        return bloodRequestRepository.findUrgentRequests();
    }

    public BloodRequest findById(Long id) {
        return bloodRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blood request not found: " + id));
    }

    public long countPendingRequests() {
        return bloodRequestRepository.countPending();
    }
}
