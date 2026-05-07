package com.trsh.kpi.data;

import com.trsh.kpi.repository.CadetRepository;
import com.trsh.kpi.repository.FlagEventRepository;
import com.trsh.kpi.repository.AppUserRepository;
import com.trsh.kpi.model.AppUser;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CadetRepository cadetRepo;
    private final FlagEventRepository flagEventRepo;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CadetRepository cadetRepo,
                      FlagEventRepository flagEventRepo,
                      AppUserRepository userRepo,
                      PasswordEncoder passwordEncoder) {
        this.cadetRepo       = cadetRepo;
        this.flagEventRepo   = flagEventRepo;
        this.userRepo        = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // ── Create default Admin account if none exists ───────
        // This is the only thing seeded on first startup.
        // Change the password immediately after first login.

        if (userRepo.count() == 0) {
            AppUser admin = new AppUser(
                "admin",
                passwordEncoder.encode("trsh@admin2026"),
                "ADMIN",
                "System Admin",
                "ALL"
            );
            userRepo.save(admin);
            System.out.println("─────────────────────────────────────");
            System.out.println("[TRSH] Default admin account created.");
            System.out.println("[TRSH] Username : admin");
            System.out.println("[TRSH] Password : trsh@admin2026");
            System.out.println("[TRSH] CHANGE THIS PASSWORD AFTER");
            System.out.println("[TRSH] FIRST LOGIN.");
            System.out.println("─────────────────────────────────────");
        } else {
            System.out.println("[TRSH] Users already exist. Skipping seed.");
        }

        System.out.println("[TRSH] System ready at http://localhost:8080");
    }
}