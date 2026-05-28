package tech.iraelie.kubescope.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.iraelie.kubescope.IntegrationTestSupport;
import tech.iraelie.kubescope.security.AuthInterface;
import tech.iraelie.kubescope.security.dto.AuthResponse;
import tech.iraelie.kubescope.security.dto.LoginRequest;
import tech.iraelie.kubescope.security.dto.RegisterRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestSupport.class)
class AuthControllerTest {

    @Autowired private MockMvc mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean private AuthInterface authService;

    @Test
    void registerReturnsCreatedAndSetsCookies() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "password123");
        when(authService.register(any())).thenReturn(new AuthResponse("at", "rt"));

        mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "not-an-email", "password123");

        mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "short");

        mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsOkAndSetsCookies() throws Exception {
        LoginRequest req = new LoginRequest("alice@example.com", "password123");
        when(authService.login(any())).thenReturn(new AuthResponse("at", "rt"));

        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void refreshWithCookieDelegatesToService() throws Exception {
        when(authService.refresh(any())).thenReturn(new AuthResponse("a", "r"));

        mvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "the-refresh")))
                .andExpect(status().isOk());

        verify(authService).refresh(any());
    }

    @Test
    @WithMockUser
    void logoutClearsCookies() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));
    }
}
