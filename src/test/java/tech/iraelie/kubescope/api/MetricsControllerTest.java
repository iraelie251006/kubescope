package tech.iraelie.kubescope.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.iraelie.kubescope.IntegrationTestSupport;
import tech.iraelie.kubescope.api.dto.HistoryPoint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestSupport.class)
class MetricsControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ClusterReadService reads;

    @Test
    void historyDefaultsTo24h() throws Exception {
        when(reads.history("24h")).thenReturn(List.of(new HistoryPoint(
                Instant.parse("2025-01-01T00:00:00Z"),
                new BigDecimal("0.10"), new BigDecimal("72.00"),
                100L, 200L, 1000L, 2000L)));

        mvc.perform(get("/api/v1/metrics/history").with(user("u")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalMonthlyCostUsd").value(72.00));

        verify(reads).history("24h");
    }

    @Test
    void historyAcceptsExplicitRange() throws Exception {
        when(reads.history("7d")).thenReturn(List.of());

        mvc.perform(get("/api/v1/metrics/history").param("range", "7d").with(user("u")))
                .andExpect(status().isOk());

        verify(reads).history("7d");
    }
}
