package tech.iraelie.kubescope.domain.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;

    @Test
    void existsByEmailReturnsTrueForSavedUser() {
        userRepository.save(User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("hash")
                .role(Role.ADMIN)
                .build());

        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void findByEmailReturnsUserWhenPresent() {
        userRepository.save(User.builder()
                .name("Bob")
                .email("bob@example.com")
                .password("hash")
                .role(Role.ADMIN)
                .build());

        assertThat(userRepository.findByEmail("bob@example.com"))
                .isPresent()
                .get()
                .extracting(User::getName)
                .isEqualTo("Bob");
    }

    @Test
    void findByEmailReturnsEmptyForMissingUser() {
        assertThat(userRepository.findByEmail("ghost@example.com")).isEmpty();
    }
}
