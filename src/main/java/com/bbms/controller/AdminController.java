package com.bbms.controller;

import com.bbms.model.*;
import com.bbms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * AdminController — UC6: Process Blood Request, UC7: Allocate Blood,
 *                    UC10: Manage Inventory, UC11: Generate Reports.
 * Owned by the team member responsible for the Admin module.
 * GRASP: Controller — admin orchestrates multi-domain operations.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BloodRequestService bloodRequestService;
    private final BloodService bloodService;
    private final UserService userService;

    // ===== Dashboard =====
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pendingRequests",
                bloodRequestService.getRequestsByStatus(BloodRequest.RequestStatus.FORWARDED));
        model.addAttribute("inventory", bloodService.getAllInventory());
        model.addAttribute("pendingTests", bloodService.countPendingTests());
        model.addAttribute("safeUnits", bloodService.countSafeUnits());
        model.addAttribute("urgentRequests", bloodRequestService.getUrgentRequests());
        return "admin/dashboard";
    }

    // ===== UC6: Approve / Reject Blood Request =====
    @GetMapping("/requests")
    public String viewRequests(Model model) {
        model.addAttribute("forwardedRequests",
                bloodRequestService.getRequestsByStatus(BloodRequest.RequestStatus.FORWARDED));
        model.addAttribute("approvedRequests",
                bloodRequestService.getRequestsByStatus(BloodRequest.RequestStatus.APPROVED));
        model.addAttribute("allRequests", bloodRequestService.getAllRequests());
        return "admin/requests";
    }

    @PostMapping("/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id,
                                 @RequestParam(defaultValue = "Approved by administrator.") String notes,
                                 RedirectAttributes ra) {
        try {
            bloodRequestService.approveRequest(id, notes);
            ra.addFlashAttribute("success", "Request #" + id + " approved.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/requests";
    }

    @PostMapping("/requests/{id}/reject")
    public String rejectRequest(@PathVariable Long id,
                                @RequestParam String reason,
                                RedirectAttributes ra) {
        try {
            bloodRequestService.rejectRequest(id, reason);
            ra.addFlashAttribute("success", "Request #" + id + " rejected.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/requests";
    }

    // ===== UC7: Allocate Blood =====
    @GetMapping("/allocate/{requestId}")
    public String allocationPage(@PathVariable Long requestId, Model model) {
        BloodRequest request = bloodRequestService.findById(requestId);
        List<BloodUnit> available = bloodService.getAvailableUnits(request.getBloodGroup());
        List<Donor> suitableDonors = bloodService.getSuitableDonors(request.getBloodGroup());
        model.addAttribute("request", request);
        model.addAttribute("availableUnits", available);
        model.addAttribute("suitableDonors", suitableDonors);
        return "admin/allocate";
    }

    @PostMapping("/allocate/{requestId}")
    public String allocateBlood(@PathVariable Long requestId,
                                @RequestParam Long bloodUnitId,
                                RedirectAttributes ra) {
        try {
            bloodRequestService.allocateBlood(requestId, bloodUnitId);
            ra.addFlashAttribute("success", "Blood allocated to request #" + requestId);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/requests";
    }

    // ===== UC10: Inventory Management =====
    @GetMapping("/inventory")
    public String inventory(Model model) {
        model.addAttribute("inventory", bloodService.getAllInventory());
        model.addAttribute("allUnits", bloodService.getAllBloodUnits());
        return "admin/inventory";
    }

    // ===== Collect blood from donor =====
    @PostMapping("/collect/{donorId}")
    public String collectBlood(@PathVariable Long donorId, RedirectAttributes ra) {
        try {
            bloodService.collectBlood(donorId);
            ra.addFlashAttribute("success", "Blood collected and sent for testing.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/inventory";
    }

    // ===== UC11: Generate Reports =====
    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("inventory", bloodService.getAllInventory());
        model.addAttribute("allRequests", bloodRequestService.getAllRequests());
        model.addAttribute("allUnits", bloodService.getAllBloodUnits());
        model.addAttribute("allUsers", userService.getAllUsers());
        model.addAttribute("safeUnits", bloodService.countSafeUnits());
        model.addAttribute("pendingRequests", bloodRequestService.countPendingRequests());
        return "admin/reports";
    }

    // ===== Select suitable donors =====
    @GetMapping("/donors")
    public String viewDonors(@RequestParam(required = false) String bloodGroup, Model model) {
        if (bloodGroup != null && !bloodGroup.isEmpty()) {
            model.addAttribute("donors", bloodService.getSuitableDonors(bloodGroup));
            model.addAttribute("selectedGroup", bloodGroup);
        }
        model.addAttribute("bloodGroups", new String[]{"A+","A-","B+","B-","O+","O-","AB+","AB-"});
        return "admin/donors";
    }
}
