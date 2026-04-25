package com.bbms.repository;

import com.bbms.model.BloodUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BloodUnitRepository extends JpaRepository<BloodUnit, Long> {
    List<BloodUnit> findByBloodGroupAndAvailableTrue(String bloodGroup);
    List<BloodUnit> findByTestStatus(BloodUnit.TestStatus status);

    @Query("SELECT b FROM BloodUnit b WHERE b.available = true AND b.bloodGroup = :bloodGroup AND b.expiryDate >= :today ORDER BY b.expiryDate ASC")
    List<BloodUnit> findAvailableByBloodGroupOrderByExpiry(String bloodGroup, LocalDate today);

    @Query("SELECT COUNT(b) FROM BloodUnit b WHERE b.testStatus = 'PENDING'")
    long countPendingTests();

    List<BloodUnit> findByAvailableTrue();
}
