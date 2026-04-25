package com.bbms.repository;

import com.bbms.model.BloodRequest;
import com.bbms.model.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
    List<BloodRequest> findByRecipient(Recipient recipient);
    List<BloodRequest> findByStatus(BloodRequest.RequestStatus status);
    List<BloodRequest> findByBloodGroup(String bloodGroup);
    List<BloodRequest> findByRecipientOrderByCreatedAtDesc(Recipient recipient);

    @Query("SELECT COUNT(r) FROM BloodRequest r WHERE r.status = 'PENDING'")
    long countPending();

    @Query("SELECT r FROM BloodRequest r WHERE r.urgency IN ('URGENT','CRITICAL') AND r.status NOT IN ('FULFILLED','REJECTED') ORDER BY r.createdAt ASC")
    List<BloodRequest> findUrgentRequests();
}
