package tech.iraelie.kubescope.nameSpaceCost;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "namespace_cost_snapshot")
public class NamespaceCostSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String namespace;

    @Column(name = "pod_count", nullable = false)
    private int podCount;

    @Column(name = "cpu_usage_millicores")
    private Long cpuUsageMillicores;

    @Column(name = "memory_usage_bytes")
    private Long memoryUsageBytes;

    @Column(name = "estimated_monthly_cost_usd", precision = 12, scale = 2)
    private BigDecimal estimatedMonthlyCostUsd;
}
