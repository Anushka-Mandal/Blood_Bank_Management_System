package com.bbms.controller;

import com.bbms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthController — UC1: Register User, UC2: Login.
 * MVC Controller: handles HTTP, delegates to UserService.
 * GRASP: Controller — system operation handler for auth use cases.
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("error", "Invalid credentials or account not approved yet.");
        if (logout != null) model.addAttribute("success", "Logged out successfully.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("bloodGroups", new String[]{"A+","A-","B+","B-","O+","O-","AB+","AB-"});
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String name,
                               @RequestParam int age,
                               @RequestParam String nid,
                               @RequestParam String bloodGroup,
                               @RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String role,
                               RedirectAttributes redirectAttributes) {
        try {
            if ("DONOR".equals(role)) {
                userService.registerDonor(name, age, nid, bloodGroup, username, password);
            } else {
                userService.registerRecipient(name, age, nid, bloodGroup, username, password);
            }
            redirectAttributes.addFlashAttribute("success",
                "Registration successful! Your account is pending approval by the System Maintainer.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
}
