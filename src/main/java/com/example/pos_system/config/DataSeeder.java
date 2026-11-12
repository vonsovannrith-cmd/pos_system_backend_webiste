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
        // បង្កើត Admin ដោយផ្អែកលើ application.properties / application.yml
        createIfMissing(fullName, email, password, Role.ADMIN);

        // ⚠️ សម្រាប់តេស្តប៉ុណ្ណោះ — លុបចេញមុន Deploy (Phase 6)
        createIfMissing("Test Cashier", "cashier@pos.com", "Cashier@12345", Role.CASHIER);
    }

    private void createIfMissing(String name, String mail, String rawPassword, Role role) {
        if (userRepository.existsByEmail(mail)) {
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