package com.trsh.kpi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Login username — must be unique
    @Column(nullable = false, unique = true)
    private String username;

    // Bcrypt hashed password — never stored as plain text
    @Column(nullable = false)
    private String password;

    // ADMIN or LIAISON
    @Column(nullable = false)
    private String role;

    // Full display name e.g. "Ms Dlamini"
    @Column(nullable = false)
    private String fullName;

    // Which region this user manages
    // ADMIN = "ALL", LIAISON = "Gauteng" / "KwaZulu-Natal" etc
    @Column(nullable = false)
    private String region;

    // Whether this account is active
    private boolean active;

    // ── Constructors ──────────────────────────────────────────

    public AppUser() {}

    public AppUser(String username, String password, String role,
                   String fullName, String region) {
        this.username = username;
        this.password = password;
        this.role     = role;
        this.fullName = fullName;
        this.region   = region;
        this.active   = true;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // ── Helper methods ────────────────────────────────────────

    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    public boolean isLiaison() {
        return "LIAISON".equals(this.role);
    }

    // Admins see all regions, Liaisons see only their own
    public boolean canAccessRegion(String region) {
        if (isAdmin()) return true;
        return this.region.equals(region);
    }
}