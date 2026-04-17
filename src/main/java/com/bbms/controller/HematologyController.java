package com.bbms.controller;

import com.bbms.service.BloodService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * HematologyController — UC9: Test Blood, UC9b: Update Test Results.
 * Owned by team member responsible for Hematology module.
 * GRASP: Controller — single point of entry for all hematology actions.
 */
@Controller
@RequestMapping("/hematology")
@RequiredArgsConstructor
public class HematologyController {

    private final BloodService bloodService;

    // ===== Dashboard =====
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pendingUnits", bloodService.getBloodUnitsPendingTest());
        model.addAttribute("allUnits", bloodService.getAllBloodUnits());
        model.addAttribute("pendingCount", bloodService.countPendingTests());
        return "hematology/dashboard";
    }

    // ===== UC9: View pending blood units =====
    @GetMapping("/test")
    public String testQueue(Model model) {
        model.addAttribute("pendingUnits", bloodService.getBloodUnitsPendingTest());
        return "hematology/test";
    }

    // ===== UC9: Mark blood as SAFE =====
    @PostMapping("/test/{unitId}/safe")
    public String markSafe(@PathVariable Long unitId,
                           @RequestParam String notes,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes ra) {
        try {
            bloodService.markBloodSafe(unitId, userDetails.getUsername(), notes);
            ra.addFlashAttribute("success", "Blood unit #" + unitId + " marked SAFE and added to inventory.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hematology/test";
    }

    // ===== UC9: Mark blood as UNSAFE =====
    @PostMapping("/test/{unitId}/unsafe")
    public String markUnsafe(@PathVariable Long unitId,
                             @RequestParam String notes,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes ra) {
        try {
            bloodService.markBloodUnsafe(unitId, userDetails.getUsername(), notes);
            ra.addFlashAttribute("success", "Blood unit #" + unitId + " marked UNSAFE and discarded.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/hematology/test";
    }

    // ===== View all test results =====
    @GetMapping("/results")
    public String results(Model model) {
        model.addAttribute("allUnits", bloodService.getAllBloodUnits());
        return "hematology/results";
    }
}
