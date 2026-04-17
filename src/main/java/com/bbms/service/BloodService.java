package com.bbms.service;

import com.bbms.model.*;
import com.bbms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BloodService - Handles:
 *   UC8:  Donor Submits Donation Details
 *   UC9:  Hematology Tests Blood (markSafe / markUnsafe)
 *   UC10: Inventory Management
 *
 * GRASP: Creator — BloodService creates BloodUnit instances.
 * GRASP: Information Expert — owns all blood processing logic.
 * Design Pattern: Builder-like construction of BloodUnit via collectBlood().
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BloodService {

    private final BloodUnitRepository bloodUnitRepository;
    private final InventoryRepository inventoryRepository;
    private final DonorRepository donorRepository;

    // ===== UC8: Donor schedules donation =====
    public Donor submitDonationDetails(Long donorId, LocalDate donationDate, String medicalNotes) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new RuntimeException("Donor not found: " + donorId));
        if (!donor.isApproved()) {
            throw new RuntimeException("Account must be approved before scheduling donation.");
        }
        donor.submitDonationDetails(donationDate, medicalNotes);
        return donorRepository.save(donor);
    }

    // ===== Collect blood from donor (Admin action after donor arrives) =====
    public BloodUnit collectBlood(Long donorId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new RuntimeException("Donor not found"));

        // Build a BloodUnit
        BloodUnit unit = new BloodUnit();
        unit.setBloodGroup(donor.getBloodGroup());
        unit.setDonor(donor);
        unit.setTestStatus(BloodUnit.TestStatus.PENDING);
        unit.setExpiryDate(LocalDate.now().plusDays(35)); // Blood expires in ~35 days
        unit.setCollectedAt(LocalDateTime.now());
        unit.setAvailable(false);

        donor.setDonationStatus(Donor.DonationStatus.PENDING_TEST);
        donorRepository.save(donor);

        return bloodUnitRepository.save(unit);
    }

    // ===== UC9: Hematology marks blood as SAFE =====
    public BloodUnit markBloodSafe(Long bloodUnitId, String testedBy, String notes) {
        BloodUnit unit = findBloodUnitById(bloodUnitId);
        unit.markSafe(testedBy, notes);

        // Update donor status
        if (unit.getDonor() != null) {
            unit.getDonor().approveMedically();
            donorRepository.save(unit.getDonor());
        }

        // Update inventory
        updateInventory(unit.getBloodGroup(), 1);

        return bloodUnitRepository.save(unit);
    }

    // ===== UC9: Hematology marks blood as UNSAFE =====
    public BloodUnit markBloodUnsafe(Long bloodUnitId, String testedBy, String notes) {
        BloodUnit unit = findBloodUnitById(bloodUnitId);
        unit.markUnsafe(testedBy, notes);

        if (unit.getDonor() != null) {
            unit.getDonor().rejectDonation();
            donorRepository.save(unit.getDonor());
        }

        return bloodUnitRepository.save(unit);
    }

    // ===== UC10: Inventory management =====
    private void updateInventory(String bloodGroup, int addCount) {
        Inventory inventory = inventoryRepository.findByBloodGroup(bloodGroup)
                .orElseGet(() -> {
                    Inventory newInv = new Inventory(bloodGroup);
                    return inventoryRepository.save(newInv);
                });
        inventory.addUnits(addCount);
        inventoryRepository.save(inventory);
    }

    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    public List<BloodUnit> getBloodUnitsPendingTest() {
        return bloodUnitRepository.findByTestStatus(BloodUnit.TestStatus.PENDING);
    }

    public List<BloodUnit> getAvailableUnits(String bloodGroup) {
        return bloodUnitRepository.findAvailableByBloodGroupOrderByExpiry(bloodGroup, LocalDate.now());
    }

    public List<BloodUnit> getAllBloodUnits() {
        return bloodUnitRepository.findAll();
    }

    public BloodUnit findBloodUnitById(Long id) {
        return bloodUnitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blood unit not found: " + id));
    }

    public List<Donor> getSuitableDonors(String bloodGroup) {
        return donorRepository.findSuitableDonors(bloodGroup);
    }

    public long countPendingTests() {
        return bloodUnitRepository.countPendingTests();
    }

    // ===== Report generation data =====
    public long countSafeUnits() {
        return bloodUnitRepository.findByAvailableTrue().size();
    }
}
