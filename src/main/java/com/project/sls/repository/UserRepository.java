package com.project.sls.repository;

import com.project.sls.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Spring Security login-hoz: email alapján megkeresi a usert
    Optional<User> findByEmail(String email);

    // Ellenőrzi, hogy létezik-e már ez az email (regisztrációnál)
    boolean existsByEmail(String email);
}
