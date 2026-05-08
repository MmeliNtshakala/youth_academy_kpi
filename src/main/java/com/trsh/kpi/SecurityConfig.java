package com.trsh.kpi;

import com.trsh.kpi.model.AppUser;
import com.trsh.kpi.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AppUserRepository userRepo;

    public SecurityConfig(AppUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // ── Password encoder ──────────────────────────────────────
    // BCrypt automatically salts and hashes passwords.
    // Never store plain text passwords.

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── User details service ──────────────────────────────────
    // Spring Security calls this on every login attempt.
    // We load the user from the database by username.

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException(
                        "User not found: " + username
                    )
                );

            if (!user.isActive()) {
                throw new UsernameNotFoundException(
                    "Account is deactivated: " + username
                );
            }

            return new User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole())
                )
            );
        };
    }

    // ── Security filter chain ─────────────────────────────────
    // Defines which URLs are protected and how login works.

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                // Login page is public
                .requestMatchers("/login", "/login?error").permitAll()

                // H2 console — dev only
                .requestMatchers("/h2-console/**").permitAll()

                // Static files — CSS, JS
                .requestMatchers("/style.css", "/app.js").permitAll()

                // Admin-only endpoints
                .requestMatchers("/api/users/**")
                    .hasRole("ADMIN")
                .requestMatchers("/api/cadets/*/delete")
                    .hasRole("ADMIN")

                // Everything else requires login
                .anyRequest().authenticated()
            )

            // ── Login config ──────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )

            // ── Logout config ─────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // Allow H2 console frames in dev
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )

            // Disable CSRF for API calls from our JS frontend
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/h2-console/**", "/logout")
            );

        return http.build();
    }
}