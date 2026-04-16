package com.trsh.kpi.data;

import com.trsh.kpi.model.Cadet;
import com.trsh.kpi.model.FlagEvent;
import com.trsh.kpi.reposatory.CadetRepository;
import com.trsh.kpi.reposatory.FlagEventRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CadetRepository cadetRepo;
    private final FlagEventRepository flagEventRepo;

    public DataSeeder(CadetRepository cadetRepo,
                      FlagEventRepository flagEventRepo) {
        this.cadetRepo = cadetRepo;
        this.flagEventRepo = flagEventRepo;
    }

    // ── Name pools ────────────────────────────────────────────

    private static final String[] FIRST_NAMES = {
        "Thabo", "Lerato", "Sipho", "Nomsa", "Ayanda",
        "Bongani", "Zanele", "Mpho", "Lungelo", "Nandi",
        "Tebogo", "Siyanda", "Kagiso", "Palesa", "Lethiwe",
        "Rethabile", "Sandile", "Dineo", "Khaya", "Ntombi",
        "Lesedi", "Sifiso", "Refilwe", "Buyani", "Nthabiseng",
        "Thandeka", "Sibonelo", "Mamello", "Kwanele", "Yethu",
        "Amogelang", "Boitumelo", "Cebisile", "Duduzile", "Enhle",
        "Fortunate", "Gugulethu", "Hlengiwe", "Itumeleng", "Jabulile"
    };

    private static final String[] LAST_NAMES = {
        "Dlamini", "Nkosi", "Mokoena", "Sithole", "Mthembu",
        "Mahlangu", "Zulu", "Ndlovu", "Mabasa", "Khumalo",
        "Mabunda", "Sibiya", "Molefe", "Nkuna", "Masondo",
        "Hadebe", "Zwane", "Madlala", "Cele", "Nxumalo",
        "Shabalala", "Buthelezi", "Vilakazi", "Ntuli", "Mkhize",
        "Radebe", "Mthethwa", "Gumede", "Ngcobo", "Mnguni"
    };

    private static final String[] PROJECTS = {
        "Leadership", "Community", "Research", "Operations"
    };

    private static final String[] PROJECT_MANAGERS = {
        "Ms Dlamini", "Mr Khumalo", "Dr Nkosi", "Ms Mokoena"
    };

    // ── Flag distribution targets (out of 300) ────────────────
    // NONE   = 216  (72%)
    // YELLOW =  48  (16%)
    // ORANGE =  24  (8%)
    // RED    =  12  (4%)

    private static final int TARGET_YELLOW = 48;
    private static final int TARGET_ORANGE = 24;
    private static final int TARGET_RED    = 12;

    @Override
    public void run(String... args) {

        // Only seed if the database is empty
        if (cadetRepo.count() > 0) {
            System.out.println("[DataSeeder] Database already populated. Skipping.");
            return;
        }

        System.out.println("[DataSeeder] Seeding 300 cadets...");

        Random random = new Random(42); // fixed seed = consistent data
        List<Cadet> cadets = new ArrayList<>();

        int yellowCount = 0;
        int orangeCount = 0;
        int redCount    = 0;

        for (int i = 1; i <= 300; i++) {

            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName  = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String fullName  = firstName + " " + lastName;
            String cadetCode = String.format("CDT-%04d", i);
            String project   = PROJECTS[i % PROJECTS.length];
            String pm        = PROJECT_MANAGERS[i % PROJECT_MANAGERS.length];

            // Determine flag status based on targets
            String flagStatus;
            if (redCount < TARGET_RED && i % 25 == 0) {
                flagStatus = "RED";
                redCount++;
            } else if (orangeCount < TARGET_ORANGE && i % 12 == 0) {
                flagStatus = "ORANGE";
                orangeCount++;
            } else if (yellowCount < TARGET_YELLOW && i % 6 == 0) {
                flagStatus = "YELLOW";
                yellowCount++;
            } else {
                flagStatus = "NONE";
            }

            // Attendance varies realistically by flag status
            int attendance = switch (flagStatus) {
                case "RED"    -> 30 + random.nextInt(25); // 30–54%
                case "ORANGE" -> 45 + random.nextInt(25); // 45–69%
                case "YELLOW" -> 60 + random.nextInt(20); // 60–79%
                default       -> 75 + random.nextInt(23); // 75–97%
            };

            // Last contact date varies by flag status
            int daysAgo = switch (flagStatus) {
                case "RED"    -> 20 + random.nextInt(15); // 20–34 days ago
                case "ORANGE" -> 10 + random.nextInt(10); // 10–19 days ago
                case "YELLOW" -> 5  + random.nextInt(8);  // 5–12 days ago
                default       -> 1  + random.nextInt(7);  // 1–7 days ago
            };
            LocalDate lastContact = LocalDate.now().minusDays(daysAgo);

            // Days since flag issued
            int daysSinceFlag = flagStatus.equals("NONE") ? 0
                : 1 + random.nextInt(10);

            // Orange flag count (relevant for RED cadets)
            int orangeFlagCount = flagStatus.equals("RED") ? 2
                : flagStatus.equals("ORANGE") ? 1 : 0;

            boolean underCorrection = flagStatus.equals("RED");

            String reengagementPlan = flagStatus.equals("ORANGE")
                ? "Cadet to attend weekly check-ins with mentor for 4 weeks. " +
                  "Must respond to all communications within 48 hours."
                : "";

            Cadet cadet = new Cadet(
                cadetCode, fullName, project,
                flagStatus, attendance, pm, lastContact
            );
            cadet.setDaysSinceFlag(daysSinceFlag);
            cadet.setOrangeFlagCount(orangeFlagCount);
            cadet.setUnderCorrection(underCorrection);
            cadet.setReengagementPlan(reengagementPlan);

            cadets.add(cadet);
        }

        // Save all cadets in one batch
        List<Cadet> savedCadets = cadetRepo.saveAll(cadets);
        System.out.println("[DataSeeder] " + savedCadets.size() + " cadets saved.");

        // ── Seed flag events for flagged cadets ───────────────
        System.out.println("[DataSeeder] Seeding flag history...");

        List<FlagEvent> events = new ArrayList<>();

        for (Cadet cadet : savedCadets) {
            String flag = cadet.getFlagStatus();

            if (flag.equals("YELLOW") || flag.equals("ORANGE")
                    || flag.equals("RED")) {

                // Every flagged cadet has a Yellow event
                events.add(new FlagEvent(
                    cadet.getId(),
                    cadet.getFullName(),
                    "YELLOW",
                    "Yellow Flag issued: missed 2 consecutive meetings " +
                    "or no response for 14 days.",
                    cadet.getProjectManager(),
                    LocalDate.now().minusDays(cadet.getDaysSinceFlag() + 7),
                    "Cadet given 5 days to re-engage."
                ));

                events.add(new FlagEvent(
                    cadet.getId(),
                    cadet.getFullName(),
                    "NOTICE_SENT",
                    "Formal written notice issued to cadet.",
                    cadet.getProjectManager(),
                    LocalDate.now().minusDays(cadet.getDaysSinceFlag() + 7),
                    null
                ));
            }

            if (flag.equals("ORANGE") || flag.equals("RED")) {

                // Orange cadets also have an Orange escalation event
                events.add(new FlagEvent(
                    cadet.getId(),
                    cadet.getFullName(),
                    "ORANGE",
                    "Escalated to Orange Flag. No re-engagement " +
                    "within 5 days of Yellow notice. " +
                    "Cadet removed from project team.",
                    cadet.getProjectManager(),
                    LocalDate.now().minusDays(cadet.getDaysSinceFlag() + 2),
                    "Referred to Head of Mentorship & Development."
                ));

                events.add(new FlagEvent(
                    cadet.getId(),
                    cadet.getFullName(),
                    "PLAN_AGREED",
                    "Re-engagement plan agreed with mentor.",
                    "Head of Mentorship",
                    LocalDate.now().minusDays(cadet.getDaysSinceFlag()),
                    cadet.getReengagementPlan().isBlank()
                        ? "Weekly check-ins agreed."
                        : cadet.getReengagementPlan()
                ));
            }

            if (flag.equals("RED")) {

                // Red cadets have a second Orange event + Red escalation
                events.add(new FlagEvent(
                    cadet.getId(),
                    cadet.getFullName(),
                    "ORANGE",
                    "Second Orange Flag issued within 12 months.",
                    cadet.getProjectManager(),
                    LocalDate.now().minusDays(cadet.getDaysSinceFlag() + 1),
                    "Automatic escalation to Red Flag triggered."
                ));

                events.add(new FlagEvent(
                    cadet.getId(),
                    cadet.getFullName(),
                    "RED",
                    "Red Flag issued. Two Orange flags within 12 months.",
                    "System (auto-escalation)",
                    LocalDate.now().minusDays(cadet.getDaysSinceFlag()),
                    "Referred to Disciplinary Subcommittee and Youth Council. " +
                    "Cadet ineligible for new appointments until restored."
                ));
            }
        }

        flagEventRepo.saveAll(events);
        System.out.println("[DataSeeder] " + events.size() +
            " flag events saved.");
        System.out.println("[DataSeeder] Seeding complete. " +
            "App ready at http://localhost:8080");
    }
}