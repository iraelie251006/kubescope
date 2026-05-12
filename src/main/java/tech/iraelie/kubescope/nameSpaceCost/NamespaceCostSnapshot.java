package tech.iraelie.kubescope.nameSpaceCost;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public int getPodCount() { return podCount; }
    public void setPodCount(int podCount) { this.podCount = podCount; }

    public Long getCpuUsageMillicores() { return cpuUsageMillicores; }
    public void setCpuUsageMillicores(Long cpuUsageMillicores) { this.cpuUsageMillicores = cpuUsageMillicores; }

    public Long getMemoryUsageBytes() { return memoryUsageBytes; }
    public void setMemoryUsageBytes(Long memoryUsageBytes) { this.memoryUsageBytes = memoryUsageBytes; }

    public BigDecimal getEstimatedMonthlyCostUsd() { return estimatedMonthlyCostUsd; }
    public void setEstimatedMonthlyCostUsd(BigDecimal estimatedMonthlyCostUsd) { this.estimatedMonthlyCostUsd = estimatedMonthlyCostUsd; }
}
