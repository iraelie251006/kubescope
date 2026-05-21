package tech.iraelie.kubescope.api.dto;

import tech.iraelie.kubescope.domain.alertRule.AlertCondition;
import tech.iraelie.kubescope.domain.alertRule.MetricType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAlertRuleRequest(
        @NotNull MetricType metricType,
        @NotNull AlertCondition condition,
        @NotNull @DecimalMin("0.0") BigDecimal thresholdValue,
        @NotBlank @Email String notificationEmail) {
}
