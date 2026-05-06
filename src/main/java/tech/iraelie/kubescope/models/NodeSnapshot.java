package tech.iraelie.kubescope.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "node_snapshot")
public class NodeSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "node_name", nullable = false)
    private String nodeName;

    private String instanceType;
    private String region;

    private Long cpuCapacityMillicores;
    private Long memoryCapacityBytes;
    private Long memoryUsageBytes;
    private BigDecimal hourlyCostUsd;
}
