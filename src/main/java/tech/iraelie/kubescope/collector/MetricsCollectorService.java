package tech.iraelie.kubescope.collector;

import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.NodeMetrics;
import io.kubernetes.client.custom.NodeMetricsList;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.custom.PodMetricsList;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.iraelie.kubescope.domain.nameSpaceCost.NamespaceCostSnapshot;
import tech.iraelie.kubescope.domain.nameSpaceCost.NamespaceCostSnapshotRepository;
import tech.iraelie.kubescope.domain.nodeSnapshot.NodeSnapshot;
import tech.iraelie.kubescope.domain.nodeSnapshot.NodeSnapshotRepository;
import tech.iraelie.kubescope.domain.podSnapshot.PodSnapshot;
import tech.iraelie.kubescope.domain.podSnapshot.PodSnapshotRepository;
import tech.iraelie.kubescope.pricing.CostCalculator;
import tech.iraelie.kubescope.pricing.PricingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class MetricsCollectorService {

    private static final String INSTANCE_TYPE_LABEL = "node.kubernetes.io/instance-type";
    private static final String REGION_LABEL = "topology.kubernetes.io/region";

    private final CoreV1Api coreV1Api;
    private final AppsV1Api appsV1Api;
    private final Metrics metrics;
    private final NodeSnapshotRepository nodeRepo;
    private final PodSnapshotRepository podRepo;
    private final NamespaceCostSnapshotRepository nsCostRepo;
    private final PricingService pricing;

    @Value("${kubescope.pricing.default-region:us-east-1}")
    private String defaultRegion;

    @Transactional
    public void collect() throws ApiException {
        Instant ts = Instant.now();

        Map<String, NodeMetrics> nodeUsage = fetchNodeMetrics();
        Map<String, PodMetrics> podUsage = fetchPodMetrics();

        BigDecimal clusterHourlyCost = BigDecimal.ZERO;
        List<NodeSnapshot> savedNodes = new ArrayList<>();
        V1NodeList nodes = coreV1Api.listNode().execute();
        for (V1Node node : nodes.getItems()) {
            String name = node.getMetadata() != null ? node.getMetadata().getName() : null;
            NodeSnapshot snap = buildNodeSnapshot(node, nodeUsage.get(name), ts);
            nodeRepo.save(snap);
            savedNodes.add(snap);
            if (snap.getHourlyCostUsd() != null) {
                clusterHourlyCost = clusterHourlyCost.add(snap.getHourlyCostUsd());
            }
        }

        long clusterCpuUsage = 0;
        long clusterMemoryUsage = 0;
        Map<String, NamespaceAggregate> byNamespace = new HashMap<>();
        V1PodList pods = coreV1Api.listPodForAllNamespaces().execute();
        for (V1Pod pod : pods.getItems()) {
            PodSnapshot snap = buildPodSnapshot(pod, podUsage.get(podKey(pod)), ts);
            podRepo.save(snap);
            NamespaceAggregate agg = byNamespace.computeIfAbsent(snap.getNamespace(), k -> new NamespaceAggregate());
            agg.podCount++;
            if (snap.getCpuUsageMillicores() != null) {
                agg.cpuUsage += snap.getCpuUsageMillicores();
                clusterCpuUsage += snap.getCpuUsageMillicores();
            }
            if (snap.getMemoryUsageBytes() != null) {
                agg.memoryUsage += snap.getMemoryUsageBytes();
                clusterMemoryUsage += snap.getMemoryUsageBytes();
            }
        }

        BigDecimal clusterMonthlyCost = CostCalculator.monthlyFromHourly(clusterHourlyCost);
        for (Map.Entry<String, NamespaceAggregate> e : byNamespace.entrySet()) {
            NamespaceAggregate agg = e.getValue();
            BigDecimal share = CostCalculator.blendedShare(agg.cpuUsage, clusterCpuUsage, agg.memoryUsage, clusterMemoryUsage);
            BigDecimal monthly = CostCalculator.applyShare(clusterMonthlyCost, share);

            NamespaceCostSnapshot ns = new NamespaceCostSnapshot();
            ns.setTimestamp(ts);
            ns.setNamespace(e.getKey());
            ns.setPodCount(agg.podCount);
            ns.setCpuUsageMillicores(agg.cpuUsage);
            ns.setMemoryUsageBytes(agg.memoryUsage);
            ns.setEstimatedMonthlyCostUsd(monthly);
            nsCostRepo.save(ns);
        }

        // Deployments observed for trend logging; per-deployment cost is derived on read in the API layer.
        V1DeploymentList deployments = appsV1Api.listDeploymentForAllNamespaces().execute();

        log.info("Snapshot taken: {} nodes (${}/h), {} pods, {} namespaces, {} deployments",
                savedNodes.size(), clusterHourlyCost.setScale(4, RoundingMode.HALF_UP),
                pods.getItems().size(), byNamespace.size(), deployments.getItems().size());
    }

    private Map<String, NodeMetrics> fetchNodeMetrics() {
        try {
            NodeMetricsList list = metrics.getNodeMetrics();
            Map<String, NodeMetrics> by = new HashMap<>();
            for (NodeMetrics nm : list.getItems()) {
                if (nm.getMetadata() != null) {
                    by.put(nm.getMetadata().getName(), nm);
                }
            }
            return by;
        } catch (Exception e) {
            log.warn("Could not fetch node metrics from metrics-server: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, PodMetrics> fetchPodMetrics() {
        try {
            PodMetricsList list = metrics.getPodMetrics("");
            Map<String, PodMetrics> by = new HashMap<>();
            for (PodMetrics pm : list.getItems()) {
                if (pm.getMetadata() != null) {
                    by.put(pm.getMetadata().getNamespace() + "/" + pm.getMetadata().getName(), pm);
                }
            }
            return by;
        } catch (Exception e) {
            log.warn("Could not fetch pod metrics from metrics-server: {}", e.getMessage());
            return Map.of();
        }
    }

    private NodeSnapshot buildNodeSnapshot(V1Node node, NodeMetrics usage, Instant ts) {
        NodeSnapshot snap = new NodeSnapshot();
        snap.setTimestamp(ts);
        if (node.getMetadata() != null) {
            snap.setNodeName(node.getMetadata().getName());
            Map<String, String> labels = node.getMetadata().getLabels();
            if (labels != null) {
                snap.setInstanceType(labels.get(INSTANCE_TYPE_LABEL));
                snap.setRegion(labels.get(REGION_LABEL));
            }
        }
        if (node.getStatus() != null && node.getStatus().getCapacity() != null) {
            Map<String, Quantity> cap = node.getStatus().getCapacity();
            snap.setCpuCapacityMillicores(cpuMillicores(cap.get("cpu")));
            snap.setMemoryCapacityBytes(memoryBytes(cap.get("memory")));
        }
        if (usage != null && usage.getUsage() != null) {
            snap.setCpuUsageMillicores(cpuMillicores(usage.getUsage().get("cpu")));
            snap.setMemoryUsageBytes(memoryBytes(usage.getUsage().get("memory")));
        }
        String region = snap.getRegion() != null ? snap.getRegion() : defaultRegion;
        if (snap.getInstanceType() != null) {
            pricing.hourlyPriceUsd(snap.getInstanceType(), region).ifPresent(snap::setHourlyCostUsd);
        }
        return snap;
    }

    private PodSnapshot buildPodSnapshot(V1Pod pod, PodMetrics usage, Instant ts) {
        PodSnapshot snap = new PodSnapshot();
        snap.setTimestamp(ts);
        if (pod.getMetadata() != null) {
            snap.setPodName(pod.getMetadata().getName());
            snap.setNamespace(pod.getMetadata().getNamespace());
        }
        if (pod.getSpec() != null) {
            snap.setNodeName(pod.getSpec().getNodeName());
            long cpuReq = 0;
            long memReq = 0;
            if (pod.getSpec().getContainers() != null) {
                for (V1Container c : pod.getSpec().getContainers()) {
                    if (c.getResources() == null || c.getResources().getRequests() == null) continue;
                    Long cpu = cpuMillicores(c.getResources().getRequests().get("cpu"));
                    Long mem = memoryBytes(c.getResources().getRequests().get("memory"));
                    if (cpu != null) cpuReq += cpu;
                    if (mem != null) memReq += mem;
                }
            }
            snap.setCpuRequestMillicores(cpuReq);
            snap.setMemoryRequestBytes(memReq);
        }
        if (pod.getStatus() != null) {
            snap.setPhase(pod.getStatus().getPhase());
        }
        if (usage != null && usage.getContainers() != null) {
            long cpuUse = 0;
            long memUse = 0;
            for (ContainerMetrics cm : usage.getContainers()) {
                if (cm.getUsage() == null) continue;
                Long cpu = cpuMillicores(cm.getUsage().get("cpu"));
                Long mem = memoryBytes(cm.getUsage().get("memory"));
                if (cpu != null) cpuUse += cpu;
                if (mem != null) memUse += mem;
            }
            snap.setCpuUsageMillicores(cpuUse);
            snap.setMemoryUsageBytes(memUse);
        }
        return snap;
    }

    private static String podKey(V1Pod pod) {
        if (pod.getMetadata() == null) return null;
        return pod.getMetadata().getNamespace() + "/" + pod.getMetadata().getName();
    }

    private static Long cpuMillicores(Quantity q) {
        return Quantities.cpuMillicores(q);
    }

    private static Long memoryBytes(Quantity q) {
        return Quantities.memoryBytes(q);
    }

    private static final class NamespaceAggregate {
        int podCount;
        long cpuUsage;
        long memoryUsage;
    }
}
