package tech.iraelie.kubescope.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Publishes cluster, namespace, and pod-phase snapshot data as Micrometer gauges so an
 * upstream Prometheus + Grafana stack can chart and alert on the same data Kubescope
 * persists to Postgres. Values are refreshed once per collector run; tagged families
 * (namespace, phase) prune series whose key no longer appears in the latest snapshot.
 */
@Component
public class ClusterMetricsPublisher {

    private final AtomicReference<Double> nodesTotal = new AtomicReference<>(0d);
    private final AtomicReference<Double> podsTotal = new AtomicReference<>(0d);
    private final AtomicReference<Double> namespacesTotal = new AtomicReference<>(0d);
    private final AtomicReference<Double> cpuUsageMillicores = new AtomicReference<>(0d);
    private final AtomicReference<Double> cpuCapacityMillicores = new AtomicReference<>(0d);
    private final AtomicReference<Double> memoryUsageBytes = new AtomicReference<>(0d);
    private final AtomicReference<Double> memoryCapacityBytes = new AtomicReference<>(0d);
    private final AtomicReference<Double> cpuUsagePercent = new AtomicReference<>(0d);
    private final AtomicReference<Double> memoryUsagePercent = new AtomicReference<>(0d);
    private final AtomicReference<Double> hourlyCostUsd = new AtomicReference<>(0d);
    private final AtomicReference<Double> monthlyCostUsd = new AtomicReference<>(0d);

    private final GaugeFamily namespaceCpuUsage;
    private final GaugeFamily namespaceMemoryUsage;
    private final GaugeFamily namespacePods;
    private final GaugeFamily namespaceMonthlyCost;
    private final GaugeFamily podsByPhase;

    public ClusterMetricsPublisher(MeterRegistry registry) {
        Gauge.builder("kubescope_cluster_nodes", nodesTotal, AtomicReference::get)
                .description("Nodes observed in the latest collection cycle").register(registry);
        Gauge.builder("kubescope_cluster_pods", podsTotal, AtomicReference::get)
                .description("Pods observed in the latest collection cycle").register(registry);
        Gauge.builder("kubescope_cluster_namespaces", namespacesTotal, AtomicReference::get)
                .description("Namespaces observed in the latest collection cycle").register(registry);
        Gauge.builder("kubescope_cluster_cpu_usage_millicores", cpuUsageMillicores, AtomicReference::get)
                .description("Cluster-wide CPU usage, in millicores").register(registry);
        Gauge.builder("kubescope_cluster_cpu_capacity_millicores", cpuCapacityMillicores, AtomicReference::get)
                .description("Cluster-wide CPU capacity, in millicores").register(registry);
        Gauge.builder("kubescope_cluster_memory_usage_bytes", memoryUsageBytes, AtomicReference::get)
                .description("Cluster-wide memory usage, in bytes").register(registry);
        Gauge.builder("kubescope_cluster_memory_capacity_bytes", memoryCapacityBytes, AtomicReference::get)
                .description("Cluster-wide memory capacity, in bytes").register(registry);
        Gauge.builder("kubescope_cluster_cpu_usage_percent", cpuUsagePercent, AtomicReference::get)
                .description("Cluster-wide CPU usage as a percentage of capacity").register(registry);
        Gauge.builder("kubescope_cluster_memory_usage_percent", memoryUsagePercent, AtomicReference::get)
                .description("Cluster-wide memory usage as a percentage of capacity").register(registry);
        Gauge.builder("kubescope_cluster_hourly_cost_usd", hourlyCostUsd, AtomicReference::get)
                .description("Estimated hourly cost of the cluster, in USD").register(registry);
        Gauge.builder("kubescope_cluster_monthly_cost_usd", monthlyCostUsd, AtomicReference::get)
                .description("Estimated monthly cost of the cluster, in USD").register(registry);

        namespaceCpuUsage = new GaugeFamily(registry, "kubescope_namespace_cpu_usage_millicores",
                "CPU usage per namespace, in millicores");
        namespaceMemoryUsage = new GaugeFamily(registry, "kubescope_namespace_memory_usage_bytes",
                "Memory usage per namespace, in bytes");
        namespacePods = new GaugeFamily(registry, "kubescope_namespace_pods",
                "Pod count per namespace");
        namespaceMonthlyCost = new GaugeFamily(registry, "kubescope_namespace_monthly_cost_usd",
                "Estimated monthly cost per namespace, in USD");
        podsByPhase = new GaugeFamily(registry, "kubescope_pods_by_phase",
                "Pod count grouped by lifecycle phase");
    }

    public void publishClusterSummary(int nodes, int pods, int namespaces,
                                       long cpuUsage, long cpuCapacity,
                                       long memUsage, long memCapacity,
                                       BigDecimal hourlyCost, BigDecimal monthlyCost) {
        nodesTotal.set((double) nodes);
        podsTotal.set((double) pods);
        namespacesTotal.set((double) namespaces);
        cpuUsageMillicores.set((double) cpuUsage);
        cpuCapacityMillicores.set((double) cpuCapacity);
        memoryUsageBytes.set((double) memUsage);
        memoryCapacityBytes.set((double) memCapacity);
        cpuUsagePercent.set(percent(cpuUsage, cpuCapacity));
        memoryUsagePercent.set(percent(memUsage, memCapacity));
        hourlyCostUsd.set(hourlyCost != null ? hourlyCost.doubleValue() : 0d);
        monthlyCostUsd.set(monthlyCost != null ? monthlyCost.doubleValue() : 0d);
    }

    public void publishNamespace(String namespace, long cpuUsage, long memUsage, int podCount, BigDecimal monthlyCost) {
        Tags tags = Tags.of("namespace", namespace);
        namespaceCpuUsage.set(tags, cpuUsage);
        namespaceMemoryUsage.set(tags, memUsage);
        namespacePods.set(tags, podCount);
        namespaceMonthlyCost.set(tags, monthlyCost != null ? monthlyCost.doubleValue() : 0d);
    }

    public void pruneNamespaces(Set<String> activeNamespaces) {
        namespaceCpuUsage.prune("namespace", activeNamespaces);
        namespaceMemoryUsage.prune("namespace", activeNamespaces);
        namespacePods.prune("namespace", activeNamespaces);
        namespaceMonthlyCost.prune("namespace", activeNamespaces);
    }

    public void publishPodPhases(Map<String, Long> countsByPhase) {
        countsByPhase.forEach((phase, count) -> podsByPhase.set(Tags.of("phase", phase), count));
        podsByPhase.prune("phase", countsByPhase.keySet());
    }

    private static double percent(long part, long total) {
        if (total <= 0) return 0d;
        return part * 100.0 / total;
    }

    private static final class GaugeFamily {
        private final MeterRegistry registry;
        private final String name;
        private final String help;
        private final Map<Tags, AtomicReference<Double>> holders = new ConcurrentHashMap<>();

        GaugeFamily(MeterRegistry registry, String name, String help) {
            this.registry = registry;
            this.name = name;
            this.help = help;
        }

        void set(Tags tags, double value) {
            holders.computeIfAbsent(tags, t -> {
                AtomicReference<Double> holder = new AtomicReference<>(value);
                Gauge.builder(name, holder, AtomicReference::get)
                        .description(help)
                        .tags(t)
                        .register(registry);
                return holder;
            }).set(value);
        }

        void prune(String tagKey, Set<String> activeValues) {
            holders.keySet().removeIf(tags -> {
                String value = tagValue(tags, tagKey);
                boolean stale = value != null && !activeValues.contains(value);
                if (stale) {
                    registry.remove(new Meter.Id(name, tags, null, null, Meter.Type.GAUGE));
                }
                return stale;
            });
        }

        private static String tagValue(Tags tags, String key) {
            for (Tag tag : tags) {
                if (tag.getKey().equals(key)) return tag.getValue();
            }
            return null;
        }
    }
}
