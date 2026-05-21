package tech.iraelie.kubescope.api;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.kubescope.api.dto.*;
import tech.iraelie.kubescope.collector.Quantities;
import tech.iraelie.kubescope.domain.nameSpaceCost.NamespaceCostSnapshot;
import tech.iraelie.kubescope.domain.nameSpaceCost.NamespaceCostSnapshotRepository;
import tech.iraelie.kubescope.domain.nodeSnapshot.NodeSnapshot;
import tech.iraelie.kubescope.domain.nodeSnapshot.NodeSnapshotRepository;
import tech.iraelie.kubescope.domain.podSnapshot.PodSnapshot;
import tech.iraelie.kubescope.domain.podSnapshot.PodSnapshotRepository;
import tech.iraelie.kubescope.pricing.CostCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClusterReadService {

    private final NodeSnapshotRepository nodeRepo;
    private final PodSnapshotRepository podRepo;
    private final NamespaceCostSnapshotRepository nsCostRepo;
    private final AppsV1Api appsV1Api;

    public ClusterOverviewResponse overview() {
        List<NodeSnapshot> nodes = nodeRepo.findLatestSnapshot();
        List<PodSnapshot> pods = podRepo.findLatestSnapshot();

        long cpuCap = 0, cpuUse = 0, memCap = 0, memUse = 0;
        BigDecimal hourly = BigDecimal.ZERO;
        for (NodeSnapshot n : nodes) {
            if (n.getCpuCapacityMillicores() != null) cpuCap += n.getCpuCapacityMillicores();
            if (n.getCpuUsageMillicores() != null) cpuUse += n.getCpuUsageMillicores();
            if (n.getMemoryCapacityBytes() != null) memCap += n.getMemoryCapacityBytes();
            if (n.getMemoryUsageBytes() != null) memUse += n.getMemoryUsageBytes();
            if (n.getHourlyCostUsd() != null) hourly = hourly.add(n.getHourlyCostUsd());
        }
        return new ClusterOverviewResponse(
                nodes.size(),
                pods.size(),
                pct(cpuUse, cpuCap),
                pct(memUse, memCap),
                CostCalculator.monthlyFromHourly(hourly));
    }

    public List<NodeResponse> nodes() {
        return nodeRepo.findLatestSnapshot().stream()
                .map(n -> new NodeResponse(
                        n.getNodeName(),
                        n.getInstanceType(),
                        n.getRegion(),
                        n.getCpuCapacityMillicores(),
                        n.getCpuUsageMillicores(),
                        n.getMemoryCapacityBytes(),
                        n.getMemoryUsageBytes(),
                        n.getHourlyCostUsd(),
                        CostCalculator.monthlyFromHourly(n.getHourlyCostUsd())))
                .sorted(Comparator.comparing(NodeResponse::name))
                .toList();
    }

    public List<NamespaceResponse> namespaces() {
        // Take the latest cost snapshot per namespace.
        Map<String, NamespaceCostSnapshot> latestPerNs = new HashMap<>();
        Instant cutoff = Instant.now().minus(2, ChronoUnit.HOURS);
        for (NamespaceCostSnapshot s : nsCostRepo.findByTimestampGreaterThanEqualOrderByTimestampDesc(cutoff)) {
            latestPerNs.putIfAbsent(s.getNamespace(), s);
        }
        return latestPerNs.values().stream()
                .map(s -> new NamespaceResponse(
                        s.getNamespace(),
                        s.getPodCount(),
                        s.getCpuUsageMillicores(),
                        s.getMemoryUsageBytes(),
                        s.getEstimatedMonthlyCostUsd()))
                .sorted(Comparator.comparing(NamespaceResponse::namespace))
                .toList();
    }

    public List<DeploymentResponse> deployments() throws ApiException {
        // Cluster monthly cost from latest node snapshots, allocate by deployment requests.
        List<NodeSnapshot> nodes = nodeRepo.findLatestSnapshot();
        BigDecimal hourly = BigDecimal.ZERO;
        long clusterCpuCap = 0, clusterMemCap = 0;
        for (NodeSnapshot n : nodes) {
            if (n.getHourlyCostUsd() != null) hourly = hourly.add(n.getHourlyCostUsd());
            if (n.getCpuCapacityMillicores() != null) clusterCpuCap += n.getCpuCapacityMillicores();
            if (n.getMemoryCapacityBytes() != null) clusterMemCap += n.getMemoryCapacityBytes();
        }
        BigDecimal clusterMonthly = CostCalculator.monthlyFromHourly(hourly);

        V1DeploymentList list = appsV1Api.listDeploymentForAllNamespaces().execute();
        List<DeploymentResponse> out = new ArrayList<>(list.getItems().size());
        for (V1Deployment d : list.getItems()) {
            int replicas = d.getSpec() != null && d.getSpec().getReplicas() != null ? d.getSpec().getReplicas() : 0;
            long cpuReq = 0, memReq = 0;
            if (d.getSpec() != null && d.getSpec().getTemplate() != null
                    && d.getSpec().getTemplate().getSpec() != null
                    && d.getSpec().getTemplate().getSpec().getContainers() != null) {
                for (V1Container c : d.getSpec().getTemplate().getSpec().getContainers()) {
                    if (c.getResources() == null || c.getResources().getRequests() == null) continue;
                    Quantity cpu = c.getResources().getRequests().get("cpu");
                    Quantity mem = c.getResources().getRequests().get("memory");
                    Long cpuM = Quantities.cpuMillicores(cpu);
                    Long memB = Quantities.memoryBytes(mem);
                    if (cpuM != null) cpuReq += cpuM;
                    if (memB != null) memReq += memB;
                }
            }
            long totalCpuReq = (long) cpuReq * replicas;
            long totalMemReq = (long) memReq * replicas;
            BigDecimal share = CostCalculator.blendedShare(totalCpuReq, clusterCpuCap, totalMemReq, clusterMemCap);
            BigDecimal monthly = CostCalculator.applyShare(clusterMonthly, share);

            String name = d.getMetadata() != null ? d.getMetadata().getName() : "unknown";
            String ns = d.getMetadata() != null ? d.getMetadata().getNamespace() : "default";
            out.add(new DeploymentResponse(name, ns, replicas, cpuReq, memReq, monthly));
        }
        out.sort(Comparator.comparing(DeploymentResponse::namespace).thenComparing(DeploymentResponse::name));
        return out;
    }

    public List<HistoryPoint> history(String range) {
        Instant since = parseRange(range);
        // Group node snapshots by timestamp; emit one HistoryPoint per snapshot batch.
        List<NodeSnapshot> snaps = nodeRepo.findByTimestampGreaterThanEqualOrderByTimestampDesc(since);
        Map<Instant, List<NodeSnapshot>> byTs = new TreeMap<>(Comparator.reverseOrder());
        for (NodeSnapshot s : snaps) {
            byTs.computeIfAbsent(s.getTimestamp(), k -> new ArrayList<>()).add(s);
        }
        List<HistoryPoint> out = new ArrayList<>(byTs.size());
        for (Map.Entry<Instant, List<NodeSnapshot>> e : byTs.entrySet()) {
            BigDecimal hourly = BigDecimal.ZERO;
            long cpuUse = 0, memUse = 0, cpuCap = 0, memCap = 0;
            for (NodeSnapshot n : e.getValue()) {
                if (n.getHourlyCostUsd() != null) hourly = hourly.add(n.getHourlyCostUsd());
                if (n.getCpuUsageMillicores() != null) cpuUse += n.getCpuUsageMillicores();
                if (n.getMemoryUsageBytes() != null) memUse += n.getMemoryUsageBytes();
                if (n.getCpuCapacityMillicores() != null) cpuCap += n.getCpuCapacityMillicores();
                if (n.getMemoryCapacityBytes() != null) memCap += n.getMemoryCapacityBytes();
            }
            out.add(new HistoryPoint(
                    e.getKey(),
                    hourly.setScale(4, RoundingMode.HALF_UP),
                    CostCalculator.monthlyFromHourly(hourly),
                    cpuUse, memUse, cpuCap, memCap));
        }
        return out;
    }

    private static Instant parseRange(String range) {
        if (range == null) range = "24h";
        Instant now = Instant.now();
        return switch (range) {
            case "24h" -> now.minus(24, ChronoUnit.HOURS);
            case "7d"  -> now.minus(7, ChronoUnit.DAYS);
            case "30d" -> now.minus(30, ChronoUnit.DAYS);
            default    -> throw new IllegalArgumentException("range must be one of 24h, 7d, 30d");
        };
    }

    private static BigDecimal pct(long part, long total) {
        if (total <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
