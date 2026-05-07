package com.trsh.kpi.controller;

import com.trsh.kpi.model.AppUser;
import com.trsh.kpi.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class AuthController {

    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AppUserRepository userRepo,
                          PasswordEncoder passwordEncoder) {
        this.userRepo        = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Login page ────────────────────────────────────────────

    /**
     * GET /login
     * Serves the login page.
     * If already logged in, redirects to dashboard.
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Authentication auth,
            Model model) {

        // Already logged in — go to dashboard
        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/";
        }

        if (error != null) {
            model.addAttribute("error",
                "Incorrect username or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("message",
                "You have been logged out successfully.");
        }

        return "login";
    }

    // ── Dashboard redirect ────────────────────────────────────

    /**
     * GET /
     * Serves the main dashboard.
     * Spring Security ensures only logged-in users reach this.
     */
    @GetMapping("/")
    public String dashboard() {
        return "forward:/index.html";
    }

    // ── Current user info ─────────────────────────────────────

    /**
     * GET /api/me
     * Returns the currently logged-in user's details.
     * Called by the frontend on page load to know:
     * - Who is logged in (name)
     * - What role they have (ADMIN or LIAISON)
     * - Which region they manage
     * This is how the frontend decides what to show/hide.
     */
    @GetMapping("/api/me")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Optional<AppUser> userOpt =
            userRepo.findByUsername(auth.getName());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        AppUser user = userOpt.get();
        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("fullName", user.getFullName());
        result.put("role",     user.getRole());
        result.put("region",   user.getRegion());
        result.put("isAdmin",  user.isAdmin());

        return ResponseEntity.ok(result);
    }

    // ── User management (Admin only) ──────────────────────────

    /**
     * GET /api/users
     * Returns all user accounts.
     * Admin only — secured in SecurityConfig.
     */
    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<List<AppUser>> getAllUsers() {
        return ResponseEntity.ok(userRepo.findAll());
    }

    /**
     * POST /api/users
     * Creates a new Liaison account.
     * Admin only.
     *
     * Request body (JSON):
     * {
     *   "username": "ms.dlamini",
     *   "password": "secure123",
     *   "fullName": "Ms Dlamini",
     *   "role": "LIAISON",
     *   "region": "Gauteng"
     * }
     */
    @PostMapping("/api/users")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createUser(
            @RequestBody Map<String, String> body) {

        Map<String, Object> result = new HashMap<>();

        String username = body.get("username");
        String password = body.get("password");
        String fullName = body.get("fullName");
        String role     = body.getOrDefault("role", "LIAISON");
        String region   = body.getOrDefault("region", "ALL");

        // Validate required fields
        if (username == null || username.isBlank()) {
            result.put("success", false);
            result.put("message", "Username is required.");
            return ResponseEntity.badRequest().body(result);
        }
        if (password == null || password.length() < 6) {
            result.put("success", false);
            result.put("message",
                "Password must be at least 6 characters.");
            return ResponseEntity.badRequest().body(result);
        }
        if (fullName == null || fullName.isBlank()) {
            result.put("success", false);
            result.put("message", "Full name is required.");
            return ResponseEntity.badRequest().body(result);
        }

        // Check username is not already taken
        if (userRepo.existsByUsername(username)) {
            result.put("success", false);
            result.put("message",
                "Username '" + username + "' is already taken.");
            return ResponseEntity.badRequest().body(result);
        }

        // Hash the password before saving
        String hashed = passwordEncoder.encode(password);

        AppUser user = new AppUser(
            username, hashed, role, fullName, region
        );
        userRepo.save(user);

        result.put("success", true);
        result.put("message", "Account created for " + fullName + ".");
        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/users/{id}/deactivate
     * Deactivates a user account without deleting it.
     * Admin only.
     */
    @PutMapping("/api/users/{id}/deactivate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deactivateUser(
            @PathVariable Long id) {

        Map<String, Object> result = new HashMap<>();
        Optional<AppUser> opt = userRepo.findById(id);

        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "User not found.");
            return ResponseEntity.notFound().build();
        }

        AppUser user = opt.get();
        if (user.isAdmin()) {
            result.put("success", false);
            result.put("message", "Cannot deactivate an Admin account.");
            return ResponseEntity.badRequest().body(result);
        }

        user.setActive(false);
        userRepo.save(user);

        result.put("success", true);
        result.put("message",
            user.getFullName() + "'s account has been deactivated.");
        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/users/{id}/activate
     * Reactivates a previously deactivated account.
     * Admin only.
     */
    @PutMapping("/api/users/{id}/activate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> activateUser(
            @PathVariable Long id) {

        Map<String, Object> result = new HashMap<>();
        Optional<AppUser> opt = userRepo.findById(id);

        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "User not found.");
            return ResponseEntity.notFound().build();
        }

        AppUser user = opt.get();
        user.setActive(true);
        userRepo.save(user);

        result.put("success", true);
        result.put("message",
            user.getFullName() + "'s account has been reactivated.");
        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/users/{id}/password
     * Resets a user's password.
     * Admin only.
     *
     * Request body:
     * { "newPassword": "newpass123" }
     */
    @PutMapping("/api/users/{id}/password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Map<String, Object> result = new HashMap<>();
        String newPassword = body.get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            result.put("success", false);
            result.put("message",
                "Password must be at least 6 characters.");
            return ResponseEntity.badRequest().body(result);
        }

        Optional<AppUser> opt = userRepo.findById(id);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "User not found.");
            return ResponseEntity.notFound().build();
        }

        AppUser user = opt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        result.put("success", true);
        result.put("message",
            "Password reset for " + user.getFullName() + ".");
        return ResponseEntity.ok(result);
    }
}