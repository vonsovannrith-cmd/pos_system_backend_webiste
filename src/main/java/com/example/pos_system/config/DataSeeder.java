package com.example.pos_system.config;

import com.example.pos_system.model.entity.User;
import com.example.pos_system.model.entity.enums.Role;
import com.example.pos_system.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.full-name}") private String fullName;
    @Value("${app.admin.email}") private String email;
    @Value("${app.admin.password}") private String password;

    @Override
    public void run(String... args) throws Exception {
        // Create an Admin based on application.properties / application.properties
        createIfMissing(fullName, email, password, Role.ADMIN);

        // ⚠️ For testing only — remove before deployment (Phase 6)
        createIfMissing("Test Cashier", "cashier@pos.com", "Cashier@12345", Role.CASHIER);
    }

    private void createIfMissing(String name, String mail, String rawPassword, Role role) {

        var existingUser = userRepository.findByEmail(mail);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            boolean passwordMatches = passwordEncoder.matches(
                    rawPassword,
                    user.getPassword()
            );

            if (!passwordMatches) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);

                log.info("Updated password for existing user: {}", mail);
            }

            return;
        }

        userRepository.save(User.builder()
                .fullName(name)
                .email(mail)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build());

        log.info("Created user: {}", mail);
    }
}