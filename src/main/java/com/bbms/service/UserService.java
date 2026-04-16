package com.bbms.service;

import com.bbms.model.*;
import com.bbms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * UserService - Handles all user-related use cases:
 *   UC1: Register User (Donor / Recipient)
 *   UC2: Verify User (Maintainer)
 *   UC3: Authenticate (Spring Security)
 *
 * GRASP: Controller — mediates between UI and domain model.
 * GRASP: Low Coupling — only depends on repository and encoder.
 * SOLID: Single Responsibility — only user lifecycle management.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final PasswordEncoder passwordEncoder;

    // ===== UC1: Register Donor =====
    public Donor registerDonor(String name, int age, String nid, String bloodGroup,
                                String username, String rawPassword) {
        validateUniqueUser(username, nid);
        Donor donor = new Donor();
        donor.setName(name);
        donor.setAge(age);
        donor.setNid(nid);
        donor.setBloodGroup(bloodGroup);
        donor.setUsername(username);
        donor.setPassword(passwordEncoder.encode(rawPassword));
        donor.setRole(User.Role.DONOR);
        donor.setAccountStatus(User.AccountStatus.PENDING);
        return donorRepository.save(donor);
    }

    // ===== UC1: Register Recipient =====
    public Recipient registerRecipient(String name, int age, String nid, String bloodGroup,
                                        String username, String rawPassword) {
        validateUniqueUser(username, nid);
        Recipient recipient = new Recipient();
        recipient.setName(name);
        recipient.setAge(age);
        recipient.setNid(nid);
        recipient.setBloodGroup(bloodGroup);
        recipient.setUsername(username);
        recipient.setPassword(passwordEncoder.encode(rawPassword));
        recipient.setRole(User.Role.RECIPIENT);
        recipient.setAccountStatus(User.AccountStatus.PENDING);
        return (Recipient) userRepository.save(recipient);
    }

    // ===== UC2: Verify User (Maintainer Action) =====
    public void approveUser(Long userId) {
        User user = findById(userId);
        user.setAccountStatus(User.AccountStatus.APPROVED);
        userRepository.save(user);
    }

    public void rejectUser(Long userId) {
        User user = findById(userId);
        user.setAccountStatus(User.AccountStatus.REJECTED);
        userRepository.save(user);
    }

    // ===== Queries =====
    public List<User> getPendingUsers() {
        return userRepository.findByAccountStatus(User.AccountStatus.PENDING);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public long countPendingUsers() {
        return userRepository.countPendingUsers();
    }

    // ===== Validation =====
    private void validateUniqueUser(String username, String nid) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already taken: " + username);
        }
        if (userRepository.existsByNid(nid)) {
            throw new RuntimeException("NID already registered: " + nid);
        }
    }
}
