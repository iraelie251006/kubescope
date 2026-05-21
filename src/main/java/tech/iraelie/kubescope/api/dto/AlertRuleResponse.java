package tech.iraelie.kubescope.api.dto;

import tech.iraelie.kubescope.domain.alertRule.AlertCondition;
import tech.iraelie.kubescope.domain.alertRule.AlertRule;
import tech.iraelie.kubescope.domain.alertRule.MetricType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertRuleResponse(
        UUID id,
        MetricType metricType,
        AlertCondition condition,
        BigDecimal thresholdValue,
        String notificationEmail,
        Instant lastFiredAt,
        Instant createdAt) {

    public static AlertRuleResponse of(AlertRule r) {
        return new AlertRuleResponse(
                r.getId(),
                r.getMetricType(),
                r.getCondition(),
                r.getThresholdValue(),
                r.getNotificationEmail(),
                r.getLastFiredAt(),
                r.getCreatedAt());
    }
}
