package tech.iraelie.kubescope.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.iraelie.kubescope.IntegrationTestSupport;
import tech.iraelie.kubescope.api.dto.CreateAlertRuleRequest;
import tech.iraelie.kubescope.domain.alertRule.AlertCondition;
import tech.iraelie.kubescope.domain.alertRule.AlertRule;
import tech.iraelie.kubescope.domain.alertRule.AlertRuleRepository;
import tech.iraelie.kubescope.domain.alertRule.MetricType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestSupport.class)
class AlertControllerTest {

    @Autowired private MockMvc mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean private AlertRuleRepository repo;

    private AlertRule sampleRule() {
        AlertRule r = new AlertRule();
        r.setId(UUID.randomUUID());
        r.setMetricType(MetricType.MONTHLY_COST);
        r.setCondition(AlertCondition.GREATER_THAN);
        r.setThresholdValue(new BigDecimal("100.00"));
        r.setNotificationEmail("ops@example.com");
        r.setCreatedAt(Instant.now());
        return r;
    }

    @Test
    void listReturnsAllRulesAsDtos() throws Exception {
        when(repo.findAll()).thenReturn(List.of(sampleRule()));

        mvc.perform(get("/api/v1/alerts").with(user("u")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metricType").value("MONTHLY_COST"))
                .andExpect(jsonPath("$[0].notificationEmail").value("ops@example.com"));
    }

    @Test
    void createReturnsCreatedRule() throws Exception {
        CreateAlertRuleRequest req = new CreateAlertRuleRequest(
                MetricType.CPU_USAGE_PERCENT,
                AlertCondition.GREATER_THAN,
                new BigDecimal("80.00"),
                "ops@example.com");
        AlertRule saved = sampleRule();
        saved.setMetricType(MetricType.CPU_USAGE_PERCENT);
        saved.setThresholdValue(new BigDecimal("80.00"));
        when(repo.save(any(AlertRule.class))).thenReturn(saved);

        mvc.perform(post("/api/v1/alerts")
                        .with(csrf())
                        .with(user("u"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metricType").value("CPU_USAGE_PERCENT"));
    }

    @Test
    void createRejectsInvalidEmail() throws Exception {
        CreateAlertRuleRequest req = new CreateAlertRuleRequest(
                MetricType.CPU_USAGE_PERCENT,
                AlertCondition.GREATER_THAN,
                new BigDecimal("80.00"),
                "not-an-email");

        mvc.perform(post("/api/v1/alerts")
                        .with(csrf())
                        .with(user("u"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsNegativeThreshold() throws Exception {
        CreateAlertRuleRequest req = new CreateAlertRuleRequest(
                MetricType.CPU_USAGE_PERCENT,
                AlertCondition.GREATER_THAN,
                new BigDecimal("-1.00"),
                "ops@example.com");

        mvc.perform(post("/api/v1/alerts")
                        .with(csrf())
                        .with(user("u"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteExistingRuleReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(repo.existsById(id)).thenReturn(true);

        mvc.perform(delete("/api/v1/alerts/{id}", id).with(csrf()).with(user("u")))
                .andExpect(status().isNoContent());

        verify(repo).deleteById(id);
    }

    @Test
    void deleteMissingRuleReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(repo.existsById(id)).thenReturn(false);

        mvc.perform(delete("/api/v1/alerts/{id}", id).with(csrf()).with(user("u")))
                .andExpect(status().isNotFound());
    }
}
