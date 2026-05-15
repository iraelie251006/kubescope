package tech.iraelie.kubescope.security;

package io.kubescope.security;

import io.kubescope.domain.User;
import io.kubescope.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${kubescope.admin.email:}")
    private String adminEmail;

    @Value("${kubescope.admin.password:}")
    private String adminPassword;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }
        if (users.findByEmail(adminEmail).isPresent()) {
            return;
        }
        User u = new User();
        u.setEmail(adminEmail);
        u.setPasswordHash(encoder.encode(adminPassword));
        u.setRole("ADMIN");
        users.save(u);
        log.info("Bootstrapped admin user: {}", adminEmail);
    }
}
