package com.bbms.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDateTime;

/**
 * TestResult - Composed within BloodUnit (Composition pattern from class diagram).
 * This cannot exist independently of a BloodUnit — pure composition.
 * GRASP: Information Expert — holds all test-related knowledge.
 */
@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TestResult {

    private String testedBy;        // name of hematology staff
    private boolean passed;
    private String notes;
    private LocalDateTime testedAt;

    public String getResultSummary() {
        if (passed) {
            return "SAFE - Blood unit cleared for use. Notes: " + notes;
        } else {
            return "UNSAFE - Blood unit rejected. Reason: " + notes;
        }
    }
}
