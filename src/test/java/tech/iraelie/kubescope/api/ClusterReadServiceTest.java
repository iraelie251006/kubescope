package tech.iraelie.kubescope.api;

import io.kubernetes.client.openapi.apis.AppsV1Api;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.iraelie.kubescope.api.dto.ClusterOverviewResponse;
import tech.iraelie.kubescope.api.dto.NodeResponse;
import tech.iraelie.kubescope.api.dto.NamespaceResponse;
import tech.iraelie.kubescope.domain.nameSpaceCost.NamespaceCostSnapshot;
import tech.iraelie.kubescope.domain.nameSpaceCost.NamespaceCostSnapshotRepository;
import tech.iraelie.kubescope.domain.nodeSnapshot.NodeSnapshot;
import tech.iraelie.kubescope.domain.nodeSnapshot.NodeSnapshotRepository;
import tech.iraelie.kubescope.domain.podSnapshot.PodSnapshot;
import tech.iraelie.kubescope.domain.podSnapshot.PodSnapshotRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClusterReadServiceTest {

    @Mock private NodeSnapshotRepository nodeRepo;
    @Mock private PodSnapshotRepository podRepo;
    @Mock private NamespaceCostSnapshotRepository nsCostRepo;
    @Mock private AppsV1Api appsV1Api;

    @InjectMocks private ClusterReadService service;

    private NodeSnapshot node(long cpuCap, long cpuUse, long memCap, long memUse, String hourly) {
        NodeSnapshot n = new NodeSnapshot();
        n.setNodeName("n");
        n.setCpuCapacityMillicores(cpuCap);
        n.setCpuUsageMillicores(cpuUse);
        n.setMemoryCapacityBytes(memCap);
        n.setMemoryUsageBytes(memUse);
        if (hourly != null) n.setHourlyCostUsd(new BigDecimal(hourly));
        n.setTimestamp(Instant.now());
        return n;
    }

    @Test
    void overviewAggregatesNodesAndPods() {
        when(nodeRepo.findLatestSnapshot()).thenReturn(List.of(
                node(2000, 1000, 1000, 500, "0.10"),
                node(2000, 500, 1000, 250, "0.10")));
        PodSnapshot p1 = new PodSnapshot(); p1.setPodName("a"); p1.setNamespace("default"); p1.setTimestamp(Instant.now());
        PodSnapshot p2 = new PodSnapshot(); p2.setPodName("b"); p2.setNamespace("default"); p2.setTimestamp(Instant.now());
        when(podRepo.findLatestSnapshot()).thenReturn(List.of(p1, p2));

        ClusterOverviewResponse overview = service.overview();

        assertThat(overview.totalNodes()).isEqualTo(2);
        assertThat(overview.totalPods()).isEqualTo(2);
        // CPU usage: 1500/4000 = 37.5
        assertThat(overview.cpuUsagePercent()).isEqualByComparingTo("37.50");
        // Memory usage: 750/2000 = 37.5
        assertThat(overview.memoryUsagePercent()).isEqualByComparingTo("37.50");
        // Monthly: hourly 0.20 * 720 = 144.00
        assertThat(overview.estimatedMonthlyCostUsd()).isEqualByComparingTo("144.00");
    }

    @Test
    void overviewHandlesEmptyClusterSafely() {
        when(nodeRepo.findLatestSnapshot()).thenReturn(List.of());
        when(podRepo.findLatestSnapshot()).thenReturn(List.of());

        ClusterOverviewResponse overview = service.overview();

        assertThat(overview.totalNodes()).isZero();
        assertThat(overview.totalPods()).isZero();
        assertThat(overview.cpuUsagePercent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(overview.memoryUsagePercent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(overview.estimatedMonthlyCostUsd()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void nodesAreReturnedSortedByName() {
        NodeSnapshot n1 = node(2000, 500, 1000, 200, "0.10"); n1.setNodeName("b-node");
        NodeSnapshot n2 = node(2000, 500, 1000, 200, "0.10"); n2.setNodeName("a-node");
        when(nodeRepo.findLatestSnapshot()).thenReturn(List.of(n1, n2));

        List<NodeResponse> nodes = service.nodes();

        assertThat(nodes).extracting(NodeResponse::name).containsExactly("a-node", "b-node");
        assertThat(nodes.get(0).monthlyCostUsd()).isEqualByComparingTo("72.00");
    }

    @Test
    void namespacesPickLatestSnapshotPerNamespaceAndSortByName() {
        Instant t1 = Instant.now().minusSeconds(60);
        Instant t2 = Instant.now();
        NamespaceCostSnapshot newer = new NamespaceCostSnapshot();
        newer.setTimestamp(t2);
        newer.setNamespace("kube-system");
        newer.setPodCount(5);
        newer.setEstimatedMonthlyCostUsd(new BigDecimal("10.00"));
        NamespaceCostSnapshot older = new NamespaceCostSnapshot();
        older.setTimestamp(t1);
        older.setNamespace("kube-system");
        older.setPodCount(3);
        older.setEstimatedMonthlyCostUsd(new BigDecimal("8.00"));
        NamespaceCostSnapshot defaultNs = new NamespaceCostSnapshot();
        defaultNs.setTimestamp(t2);
        defaultNs.setNamespace("default");
        defaultNs.setPodCount(2);
        defaultNs.setEstimatedMonthlyCostUsd(new BigDecimal("4.00"));

        // Repo returns ordered-by-timestamp desc → newer comes first, then older, then defaultNs
        when(nsCostRepo.findByTimestampGreaterThanEqualOrderByTimestampDesc(any()))
                .thenReturn(List.of(newer, defaultNs, older));

        List<NamespaceResponse> result = service.namespaces();

        assertThat(result).extracting(NamespaceResponse::namespace)
                .containsExactly("default", "kube-system");
        assertThat(result).extracting(NamespaceResponse::podCount)
                .containsExactly(2, 5);
    }

    @Test
    void historyRejectsUnknownRange() {
        assertThatThrownBy(() -> service.history("100y"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void historyReturnsOnePointPerTimestampBatch() {
        Instant t1 = Instant.parse("2025-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2025-01-01T01:00:00Z");
        NodeSnapshot a = node(2000, 100, 1000, 100, "0.10"); a.setTimestamp(t1);
        NodeSnapshot b = node(2000, 200, 1000, 200, "0.10"); b.setTimestamp(t1);
        NodeSnapshot c = node(2000, 300, 1000, 300, "0.20"); c.setTimestamp(t2);
        when(nodeRepo.findByTimestampGreaterThanEqualOrderByTimestampDesc(any()))
                .thenReturn(List.of(a, b, c));

        var points = service.history("24h");

        assertThat(points).hasSize(2);
        // Newest first
        assertThat(points.get(0).timestamp()).isEqualTo(t2);
        assertThat(points.get(0).totalCpuUsageMillicores()).isEqualTo(300L);
        // t1 batch sums both nodes
        assertThat(points.get(1).timestamp()).isEqualTo(t1);
        assertThat(points.get(1).totalCpuUsageMillicores()).isEqualTo(300L);
        assertThat(points.get(1).totalHourlyCostUsd()).isEqualByComparingTo("0.2000");
    }
}
