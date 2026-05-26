package tech.iraelie.kubescope.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import tech.iraelie.kubescope.domain.user.Role;
import tech.iraelie.kubescope.domain.user.User;
import tech.iraelie.kubescope.domain.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock private UserRepository users;
    @Mock private PasswordEncoder encoder;
    @InjectMocks private AdminBootstrap bootstrap;

    @Test
    void doesNothingWhenEmailIsBlank() {
        ReflectionTestUtils.setField(bootstrap, "adminEmail", "");
        ReflectionTestUtils.setField(bootstrap, "adminPassword", "secret");

        bootstrap.run();

        verify(users, never()).findByEmail(any());
        verify(users, never()).save(any());
    }

    @Test
    void doesNothingWhenPasswordIsBlank() {
        ReflectionTestUtils.setField(bootstrap, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(bootstrap, "adminPassword", "");

        bootstrap.run();

        verify(users, never()).save(any());
    }

    @Test
    void skipsWhenAdminAlreadyExists() {
        ReflectionTestUtils.setField(bootstrap, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(bootstrap, "adminPassword", "secret");
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(new User()));

        bootstrap.run();

        verify(users, never()).save(any());
    }

    @Test
    void createsAdminUserWhenAbsent() {
        ReflectionTestUtils.setField(bootstrap, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(bootstrap, "adminPassword", "secret");
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(encoder.encode("secret")).thenReturn("encoded");

        bootstrap.run();

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(users).save(cap.capture());
        assertThat(cap.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(cap.getValue().getPassword()).isEqualTo("encoded");
        assertThat(cap.getValue().getRole()).isEqualTo(Role.ADMIN);
    }
}
