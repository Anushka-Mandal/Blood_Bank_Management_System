package com.bbms.config;

import com.bbms.model.*;
import com.bbms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DataInitializer — Seeds initial staff accounts and sample inventory.
 * Pattern: Singleton (via Spring @Component) — runs once on startup.
 * GRASP: Pure Fabrication — exists to satisfy infrastructure needs only.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create staff accounts if not already present
        createStaffIfAbsent("admin",       "Admin User",       "ADMIN001",  User.Role.ADMIN);
        createStaffIfAbsent("maintainer",  "System Maintainer","MAINT001",  User.Role.MAINTAINER);
        createStaffIfAbsent("hematology",  "Hematology Dept",  "HEMA001",   User.Role.HEMATOLOGY);

        // Initialise inventory for all blood groups
        String[] groups = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        for (String group : groups) {
            inventoryRepository.findByBloodGroup(group).orElseGet(() -> {
                Inventory inv = new Inventory(group);
                return inventoryRepository.save(inv);
            });
        }
    }

    private void createStaffIfAbsent(String username, String name, String nid, User.Role role) {
        if (userRepository.existsByUsername(username)) return;
        User staff = new User();
        staff.setUsername(username);
        staff.setName(name);
        staff.setAge(30);
        staff.setNid(nid);
        staff.setBloodGroup("O+");
        staff.setPassword(passwordEncoder.encode("password123"));
        staff.setRole(role);
        staff.setAccountStatus(User.AccountStatus.APPROVED);
        userRepository.save(staff);
    }
}
