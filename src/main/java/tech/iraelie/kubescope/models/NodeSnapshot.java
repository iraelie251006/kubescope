package tech.iraelie.kubescope.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "node_snapshot")
public class NodeSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "node_name", nullable = false)
    private String nodeName;

    @Column(name = "instance_type")
    private String instanceType;

    private String region;

    @Column(name = "cpu_capacity_millicores")
    private Long cpuCapacityMillicores;

    @Column(name = "cpu_usage_millicores")
    private Long cpuUsageMillicores;

    @Column(name = "memory_capacity_bytes")
    private Long memoryCapacityBytes;

    @Column(name = "memory_usage_bytes")
    private Long memoryUsageBytes;

    @Column(name = "hourly_cost_usd", precision = 10, scale = 4)
    private BigDecimal hourlyCostUsd;

    @Column(nullable = false)
    private Instant timestamp;
}
