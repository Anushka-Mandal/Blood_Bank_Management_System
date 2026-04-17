package com.bbms.controller;

import com.bbms.model.BloodRequest;
import com.bbms.model.User;
import com.bbms.service.BloodRequestService;
import com.bbms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * MaintainerController — UC2: Verify User, UC5: Forward Blood Request.
 * Owned by team member responsible for Maintainer module.
 * GRASP: Controller — delegates to UserService and BloodRequestService.
 */
@Controller
@RequestMapping("/maintainer")
@RequiredArgsConstructor
public class MaintainerController {

    private final UserService userService;
    private final BloodRequestService bloodRequestService;

    // ===== Dashboard =====
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pendingUsers", userService.getPendingUsers());
        model.addAttribute("pendingRequests",
                bloodRequestService.getRequestsByStatus(BloodRequest.RequestStatus.PENDING));
        model.addAttribute("pendingUserCount", userService.countPendingUsers());
        model.addAttribute("pendingRequestCount", bloodRequestService.countPendingRequests());
        return "maintainer/dashboard";
    }

    // ===== UC2: Verify Users =====
    @GetMapping("/users")
    public String viewPendingUsers(@RequestParam(defaultValue = "PENDING") String status, Model model) {
        List<User> allUsers = userService.getAllUsers();
        List<User> filteredUsers = switch (status.toUpperCase()) {
            case "APPROVED" -> allUsers.stream()
                    .filter(user -> user.getAccountStatus() == User.AccountStatus.APPROVED)
                    .toList();
            case "REJECTED" -> allUsers.stream()
                    .filter(user -> user.getAccountStatus() == User.AccountStatus.REJECTED)
                    .toList();
            case "ALL" -> allUsers;
            default -> allUsers.stream()
                    .filter(user -> user.getAccountStatus() == User.AccountStatus.PENDING)
                    .toList();
        };

        model.addAttribute("selectedStatus", status.toUpperCase());
        model.addAttribute("filteredUsers", filteredUsers);
        return "maintainer/users";
    }

    @GetMapping("/users/all")
    public String viewAllUsers(Model model) {
        model.addAttribute("allUsers", userService.getAllUsers());
        return "maintainer/all-users";
    }

    @PostMapping("/users/{id}/approve")
    public String approveUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.approveUser(id);
            ra.addFlashAttribute("success", "User account approved.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/maintainer/users";
    }

    @PostMapping("/users/{id}/reject")
    public String rejectUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.rejectUser(id);
            ra.addFlashAttribute("success", "User account rejected.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/maintainer/users";
    }

    // ===== UC5: Forward Blood Request to Admin =====
    @GetMapping("/requests")
    public String viewRequests(Model model) {
        model.addAttribute("pendingRequests",
                bloodRequestService.getRequestsByStatus(BloodRequest.RequestStatus.PENDING));
        model.addAttribute("forwardedRequests",
                bloodRequestService.getRequestsByStatus(BloodRequest.RequestStatus.FORWARDED));
        return "maintainer/requests";
    }

    @PostMapping("/requests/{id}/forward")
    public String forwardRequest(@PathVariable Long id,
                                 @RequestParam(defaultValue = "Reviewed and forwarded by maintainer.") String notes,
                                 RedirectAttributes ra) {
        try {
            bloodRequestService.forwardRequest(id, notes);
            ra.addFlashAttribute("success", "Request #" + id + " forwarded to administrator.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/maintainer/requests";
    }
}
