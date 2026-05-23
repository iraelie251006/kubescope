package tech.iraelie.kubescope.alerting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.kubescope.api.ClusterReadService;
import tech.iraelie.kubescope.api.dto.ClusterOverviewResponse;
import tech.iraelie.kubescope.domain.alertEvent.AlertEvent;
import tech.iraelie.kubescope.domain.alertEvent.AlertEventRepository;
import tech.iraelie.kubescope.domain.alertRule.AlertCondition;
import tech.iraelie.kubescope.domain.alertRule.AlertRule;
import tech.iraelie.kubescope.domain.alertRule.AlertRuleRepository;
import tech.iraelie.kubescope.domain.alertRule.MetricType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class AlertEvaluator {
    private final AlertRuleRepository ruleRepo;
    private final AlertEventRepository eventRepo;
    private final ClusterReadService reads;
    private final EmailNotifier mailer;

    @Value("${kubescope.alerts.cooldown-minutes:60}")
    private long cooldownMinutes;

    @Transactional
    public void evaluate() {
        var rules = ruleRepo.findAll();
        if (rules.isEmpty()) return;

        ClusterOverviewResponse overview = reads.overview();
        Instant now = Instant.now();
        Duration cooldown = Duration.ofMinutes(cooldownMinutes);

        for (AlertRule rule : rules) {
            BigDecimal value = currentValue(rule.getMetricType(), overview);
            if (!isBreached(rule.getCondition(), value, rule.getThresholdValue())) continue;
            if (rule.getLastFiredAt() != null
                    && Duration.between(rule.getLastFiredAt(), now).compareTo(cooldown) < 0) {
                continue;
            }

            AlertEvent event = new AlertEvent();
            event.setAlertRuleId(rule.getId());
            event.setMetricValue(value);
            event.setFiredAt(now);
            try {
                mailer.send(rule, value);
                event.setNotified(true);
            } catch (Exception e) {
                log.warn("Alert email failed for rule {}: {}", rule.getId(), e.getMessage());
                event.setNotified(false);
            }
            eventRepo.save(event);

            rule.setLastFiredAt(now);
            ruleRepo.save(rule);
            log.info("Alert fired: rule={} metric={} value={} threshold={}",
                    rule.getId(), rule.getMetricType(), value, rule.getThresholdValue());
        }
    }

    private static BigDecimal currentValue(MetricType type, ClusterOverviewResponse o) {
        return switch (type) {
            case MONTHLY_COST          -> o.estimatedMonthlyCostUsd();
            case CPU_USAGE_PERCENT     -> o.cpuUsagePercent();
            case MEMORY_USAGE_PERCENT  -> o.memoryUsagePercent();
        };
    }

    private static boolean isBreached(AlertCondition cond, BigDecimal value, BigDecimal threshold) {
        if (value == null) return false;
        return cond == AlertCondition.GREATER_THAN && value.compareTo(threshold) > 0;
    }
}
