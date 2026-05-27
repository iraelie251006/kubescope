package tech.iraelie.kubescope.domain.alertRule;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class AlertRuleRepositoryTest {

    @Autowired private AlertRuleRepository repo;

    private AlertRule newRule(MetricType type, String threshold) {
        AlertRule r = new AlertRule();
        r.setMetricType(type);
        r.setCondition(AlertCondition.GREATER_THAN);
        r.setThresholdValue(new BigDecimal(threshold));
        r.setNotificationEmail("ops@example.com");
        return r;
    }

    @Test
    void saveAssignsIdAndPreservesFields() {
        AlertRule saved = repo.save(newRule(MetricType.MONTHLY_COST, "100.00"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMetricType()).isEqualTo(MetricType.MONTHLY_COST);
        assertThat(saved.getThresholdValue()).isEqualByComparingTo("100.00");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void existsByIdMatchesPersistedRule() {
        AlertRule saved = repo.save(newRule(MetricType.CPU_USAGE_PERCENT, "80.00"));

        assertThat(repo.existsById(saved.getId())).isTrue();
        assertThat(repo.existsById(UUID.randomUUID())).isFalse();
    }

    @Test
    void deleteByIdRemovesRule() {
        AlertRule saved = repo.save(newRule(MetricType.MEMORY_USAGE_PERCENT, "85.00"));

        repo.deleteById(saved.getId());

        assertThat(repo.existsById(saved.getId())).isFalse();
    }
}
