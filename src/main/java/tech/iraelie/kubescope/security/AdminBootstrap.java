package tech.iraelie.kubescope.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tech.iraelie.kubescope.domain.user.Role;
import tech.iraelie.kubescope.domain.user.User;
import tech.iraelie.kubescope.domain.user.UserRepository;

@Slf4j
@RequiredArgsConstructor
@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${kubescope.admin.email:}")
    private String adminEmail;

    @Value("${kubescope.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }
        if (users.findByEmail(adminEmail).isPresent()) {
            return;
        }
        User u = User.builder()
                .email(adminEmail)
                .password(encoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();

        users.save(u);
        log.info("Bootstrapped admin user: {}", adminEmail);
    }
}
