package tech.iraelie.kubescope.podSnapshot;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pod_snapshot")
public class PodSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pod_name", nullable = false)
    private String podName;

    @Column(nullable = false)
    private String namespace;

    @Column(name = "node_name")
    private String nodeName;

    @Column(name = "cpu_request_millicores")
    private Long cpuRequestMillicores;

    @Column(name = "memory_request_bytes")
    private Long memoryRequestBytes;

    @Column(name = "cpu_usage_millicores")
    private Long cpuUsageMillicores;

    @Column(name = "memory_usage_bytes")
    private Long memoryUsageBytes;

    private String phase;

    @Column(nullable = false)
    private Instant timestamp;
}
