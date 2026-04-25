package com.bbms.controller;

import com.bbms.model.*;
import com.bbms.repository.UserRepository;
import com.bbms.service.BloodRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * RecipientController — UC4: Recipient Activities.
 * Owned by team member responsible for Recipient module.
 * GRASP: Controller — delegates all logic to BloodRequestService.
 */
@Controller
@RequestMapping("/recipient")
@RequiredArgsConstructor
public class RecipientController {

    private final BloodRequestService bloodRequestService;
    private final UserRepository userRepository;

    private Recipient getCurrentRecipient(UserDetails userDetails) {
        return (Recipient) userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));
    }

    // ===== Dashboard =====
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Recipient recipient = getCurrentRecipient(userDetails);
        List<BloodRequest> requests = bloodRequestService.getRequestsByRecipient(recipient);
        model.addAttribute("recipient", recipient);
        model.addAttribute("requests", requests);
        model.addAttribute("urgentCount",
                requests.stream().filter(BloodRequest::isUrgent).count());
        return "recipient/dashboard";
    }

    // ===== UC4: Request Blood =====
    @GetMapping("/request")
    public String requestForm(Model model) {
        model.addAttribute("bloodGroups", new String[]{"A+","A-","B+","B-","O+","O-","AB+","AB-"});
        return "recipient/request";
    }

    @PostMapping("/request")
    public String submitRequest(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam String bloodGroup,
                                @RequestParam int quantity,
                                @RequestParam String requiredDate,
                                @RequestParam String urgency,
                                RedirectAttributes redirectAttributes) {
        try {
            Recipient recipient = getCurrentRecipient(userDetails);
            bloodRequestService.submitRequest(
                    recipient.getUserId(), bloodGroup, quantity,
                    LocalDate.parse(requiredDate), urgency);
            redirectAttributes.addFlashAttribute("success",
                    "Blood request submitted successfully. A maintainer will review it shortly.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recipient/dashboard";
    }

    // ===== UC: Track Request Status =====
    @GetMapping("/track/{id}")
    public String trackRequest(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        BloodRequest request = bloodRequestService.findById(id);
        model.addAttribute("request", request);
        return "recipient/track";
    }
}
