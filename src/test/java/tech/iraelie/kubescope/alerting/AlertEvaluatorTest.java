package tech.iraelie.kubescope.alerting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.iraelie.kubescope.api.ClusterReadService;
import tech.iraelie.kubescope.api.dto.ClusterOverviewResponse;
import tech.iraelie.kubescope.domain.alertEvent.AlertEvent;
import tech.iraelie.kubescope.domain.alertEvent.AlertEventRepository;
import tech.iraelie.kubescope.domain.alertRule.AlertCondition;
import tech.iraelie.kubescope.domain.alertRule.AlertRule;
import tech.iraelie.kubescope.domain.alertRule.AlertRuleRepository;
import tech.iraelie.kubescope.domain.alertRule.MetricType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertEvaluatorTest {

    @Mock private AlertRuleRepository ruleRepo;
    @Mock private AlertEventRepository eventRepo;
    @Mock private ClusterReadService reads;
    @Mock private EmailNotifier mailer;
    @InjectMocks private AlertEvaluator evaluator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(evaluator, "cooldownMinutes", 60L);
    }

    private AlertRule rule(MetricType metric, String threshold) {
        AlertRule r = new AlertRule();
        r.setId(UUID.randomUUID());
        r.setMetricType(metric);
        r.setCondition(AlertCondition.GREATER_THAN);
        r.setThresholdValue(new BigDecimal(threshold));
        r.setNotificationEmail("ops@example.com");
        return r;
    }

    private ClusterOverviewResponse overview(String monthlyCost, String cpu, String mem) {
        return new ClusterOverviewResponse(
                3, 10,
                new BigDecimal(cpu),
                new BigDecimal(mem),
                new BigDecimal(monthlyCost));
    }

    @Test
    void noRulesShortCircuitsWithoutCallingReads() {
        when(ruleRepo.findAll()).thenReturn(List.of());

        evaluator.evaluate();

        verify(reads, never()).overview();
        verify(mailer, never()).send(any(), any());
    }

    @Test
    void firesAlertWhenThresholdExceeded() {
        AlertRule r = rule(MetricType.MONTHLY_COST, "100.00");
        when(ruleRepo.findAll()).thenReturn(List.of(r));
        when(reads.overview()).thenReturn(overview("150.00", "10.00", "20.00"));

        evaluator.evaluate();

        verify(mailer).send(eq(r), any(BigDecimal.class));
        ArgumentCaptor<AlertEvent> eventCap = ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventRepo).save(eventCap.capture());
        assertThat(eventCap.getValue().getAlertRuleId()).isEqualTo(r.getId());
        assertThat(eventCap.getValue().isNotified()).isTrue();
        assertThat(eventCap.getValue().getMetricValue()).isEqualByComparingTo("150.00");
        verify(ruleRepo).save(r);
        assertThat(r.getLastFiredAt()).isNotNull();
    }

    @Test
    void doesNotFireWhenThresholdNotExceeded() {
        AlertRule r = rule(MetricType.MONTHLY_COST, "100.00");
        when(ruleRepo.findAll()).thenReturn(List.of(r));
        when(reads.overview()).thenReturn(overview("50.00", "10.00", "20.00"));

        evaluator.evaluate();

        verify(mailer, never()).send(any(), any());
        verify(eventRepo, never()).save(any());
        verify(ruleRepo, never()).save(any());
    }

    @Test
    void doesNotFireDuringCooldown() {
        AlertRule r = rule(MetricType.CPU_USAGE_PERCENT, "75.00");
        r.setLastFiredAt(Instant.now().minusSeconds(30));
        when(ruleRepo.findAll()).thenReturn(List.of(r));
        when(reads.overview()).thenReturn(overview("0", "99.00", "10.00"));

        evaluator.evaluate();

        verify(mailer, never()).send(any(), any());
        verify(eventRepo, never()).save(any());
    }

    @Test
    void firesAgainAfterCooldownExpires() {
        AlertRule r = rule(MetricType.MEMORY_USAGE_PERCENT, "80.00");
        r.setLastFiredAt(Instant.now().minusSeconds(60 * 60 + 30));
        when(ruleRepo.findAll()).thenReturn(List.of(r));
        when(reads.overview()).thenReturn(overview("0", "10.00", "95.00"));

        evaluator.evaluate();

        verify(mailer).send(eq(r), any());
        verify(eventRepo).save(any(AlertEvent.class));
    }

    @Test
    void mailerFailureStillSavesEventMarkedNotNotified() {
        AlertRule r = rule(MetricType.CPU_USAGE_PERCENT, "50.00");
        when(ruleRepo.findAll()).thenReturn(List.of(r));
        when(reads.overview()).thenReturn(overview("0", "99.00", "10.00"));
        doThrow(new RuntimeException("smtp down")).when(mailer).send(any(), any());

        evaluator.evaluate();

        ArgumentCaptor<AlertEvent> cap = ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventRepo).save(cap.capture());
        assertThat(cap.getValue().isNotified()).isFalse();
        // Rule still gets a lastFiredAt to honor the cooldown even on a delivery failure
        verify(ruleRepo).save(r);
    }
}
