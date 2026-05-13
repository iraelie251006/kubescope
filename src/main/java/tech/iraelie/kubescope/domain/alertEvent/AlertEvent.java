package tech.iraelie.kubescope.domain.alertEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "alert_event")
public class AlertEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "alert_rule_id", nullable = false)
    private UUID alertRuleId;

    @Column(name = "fired_at", nullable = false)
    private Instant firedAt = Instant.now();

    @Column(name = "metric_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal metricValue;

    @Column(nullable = false)
    private boolean notified = false;
}

