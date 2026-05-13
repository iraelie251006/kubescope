package tech.iraelie.kubescope.domain.alertRule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_rule")
public class AlertRule {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false)
    private AlertCondition condition;

    @Column(name = "threshold_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal thresholdValue;

    @Column(name = "notification_email", nullable = false)
    private String notificationEmail;

    @Column(name = "last_fired_at")
    private Instant lastFiredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public MetricType getMetricType() { return metricType; }
    public void setMetricType(MetricType metricType) { this.metricType = metricType; }

    public AlertCondition getCondition() { return condition; }
    public void setCondition(AlertCondition condition) { this.condition = condition; }

    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }

    public String getNotificationEmail() { return notificationEmail; }
    public void setNotificationEmail(String notificationEmail) { this.notificationEmail = notificationEmail; }

    public Instant getLastFiredAt() { return lastFiredAt; }
    public void setLastFiredAt(Instant lastFiredAt) { this.lastFiredAt = lastFiredAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

