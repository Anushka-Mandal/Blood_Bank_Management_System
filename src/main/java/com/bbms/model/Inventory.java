package com.bbms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Inventory - Tracks blood stock levels per blood group.
 * From class diagram: Administrator → Inventory (Aggregation).
 * GRASP: High Cohesion — single responsibility: track stock.
 * Design Pattern: Singleton via Spring @Service (one inventory service manages this).
 */
@Entity
@Table(name = "inventory")
@Getter @Setter @NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryId;

    @Column(nullable = false, unique = true)
    private String bloodGroup;  // One record per blood group

    @Column(nullable = false)
    private int totalUnits = 0;

    @Column(nullable = false)
    private int availableUnits = 0;

    @Column(nullable = false)
    private int reservedUnits = 0;

    private LocalDateTime lastUpdated = LocalDateTime.now();

    public Inventory(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    // ===== Business Methods =====
    public void addUnits(int count) {
        this.totalUnits += count;
        this.availableUnits += count;
        this.lastUpdated = LocalDateTime.now();
    }

    public boolean reserveUnits(int count) {
        if (this.availableUnits >= count) {
            this.availableUnits -= count;
            this.reservedUnits += count;
            this.lastUpdated = LocalDateTime.now();
            return true;
        }
        return false;
    }

    public void fulfillReservation(int count) {
        this.reservedUnits -= count;
        this.totalUnits -= count;
        this.lastUpdated = LocalDateTime.now();
    }

    public void releaseReservation(int count) {
        this.reservedUnits -= count;
        this.availableUnits += count;
        this.lastUpdated = LocalDateTime.now();
    }

    public boolean hasSufficientStock(int required) {
        return this.availableUnits >= required;
    }

    public String getStockLevel() {
        if (availableUnits == 0) return "OUT_OF_STOCK";
        if (availableUnits <= 5) return "CRITICAL";
        if (availableUnits <= 15) return "LOW";
        return "ADEQUATE";
    }
}
