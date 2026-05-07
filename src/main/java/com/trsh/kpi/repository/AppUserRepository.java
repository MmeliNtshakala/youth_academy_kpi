package com.trsh.kpi.repository;

import com.trsh.kpi.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // Find user by username — used by Spring Security on login
    Optional<AppUser> findByUsername(String username);

    // Find all users in a specific region
    List<AppUser> findByRegion(String region);

    // Find all Liaisons
    List<AppUser> findByRole(String role);

    // Find all active accounts
    List<AppUser> findByActiveTrue();

    // Check if username already exists — used when creating accounts
    boolean existsByUsername(String username);
}