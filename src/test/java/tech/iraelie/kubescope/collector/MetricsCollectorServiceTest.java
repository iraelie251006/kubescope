package tech.iraelie.kubescope.collector;

import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.NodeMetricsList;
import io.kubernetes.client.custom.PodMetricsList;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1NodeStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Answers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.iraelie.kubescope.domain.nameSpaceCost.NamespaceCostSnapshot;
import tech.iraelie.kubescope.domain.nameSpaceCost.NamespaceCostSnapshotRepository;
import tech.iraelie.kubescope.domain.nodeSnapshot.NodeSnapshot;
import tech.iraelie.kubescope.domain.nodeSnapshot.NodeSnapshotRepository;
import tech.iraelie.kubescope.domain.podSnapshot.PodSnapshot;
import tech.iraelie.kubescope.domain.podSnapshot.PodSnapshotRepository;
import tech.iraelie.kubescope.pricing.PricingService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsCollectorServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private CoreV1Api coreV1Api;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private AppsV1Api appsV1Api;
    @Mock private Metrics metrics;
    @Mock private NodeSnapshotRepository nodeRepo;
    @Mock private PodSnapshotRepository podRepo;
    @Mock private NamespaceCostSnapshotRepository nsCostRepo;
    @Mock private PricingService pricing;

    @InjectMocks private MetricsCollectorService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultRegion", "us-east-1");
    }

    private V1Node node(String name, String instanceType, String region, String cpu, String memory) {
        V1Node n = new V1Node();
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(name);
        if (instanceType != null || region != null) {
            meta.setLabels(Map.of(
                    "node.kubernetes.io/instance-type", instanceType != null ? instanceType : "",
                    "topology.kubernetes.io/region", region != null ? region : ""));
        }
        n.setMetadata(meta);
        V1NodeStatus status = new V1NodeStatus();
        status.setCapacity(Map.of(
                "cpu", Quantity.fromString(cpu),
                "memory", Quantity.fromString(memory)));
        n.setStatus(status);
        return n;
    }

    private V1Pod pod(String name, String ns, String cpuReq, String memReq) {
        V1Pod p = new V1Pod();
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(name);
        meta.setNamespace(ns);
        p.setMetadata(meta);
        V1PodSpec spec = new V1PodSpec();
        V1Container container = new V1Container();
        container.setName("app");
        V1ResourceRequirements rr = new V1ResourceRequirements();
        rr.setRequests(Map.of(
                "cpu", Quantity.fromString(cpuReq),
                "memory", Quantity.fromString(memReq)));
        container.setResources(rr);
        spec.setContainers(List.of(container));
        p.setSpec(spec);
        V1PodStatus status = new V1PodStatus();
        status.setPhase("Running");
        p.setStatus(status);
        return p;
    }

    @Test
    void collectPersistsSnapshotsAndAttachesHourlyCost() throws Exception {
        V1NodeList nodeList = new V1NodeList();
        nodeList.setItems(List.of(node("n1", "t3.medium", "us-east-1", "2", "2Gi")));
        when(coreV1Api.listNode().execute()).thenReturn(nodeList);

        V1PodList podList = new V1PodList();
        podList.setItems(List.of(
                pod("p1", "default", "500m", "256Mi"),
                pod("p2", "default", "500m", "256Mi")));
        when(coreV1Api.listPodForAllNamespaces().execute()).thenReturn(podList);

        V1DeploymentList deps = new V1DeploymentList();
        deps.setItems(List.of());
        when(appsV1Api.listDeploymentForAllNamespaces().execute()).thenReturn(deps);

        NodeMetricsList nodeMetrics = new NodeMetricsList();
        nodeMetrics.setItems(List.of());
        when(metrics.getNodeMetrics()).thenReturn(nodeMetrics);
        PodMetricsList podMetrics = new PodMetricsList();
        podMetrics.setItems(List.of());
        when(metrics.getPodMetrics(eq(""))).thenReturn(podMetrics);

        when(pricing.hourlyPriceUsd("t3.medium", "us-east-1"))
                .thenReturn(Optional.of(new BigDecimal("0.0416")));

        service.collect();

        ArgumentCaptor<NodeSnapshot> nodeCap = ArgumentCaptor.forClass(NodeSnapshot.class);
        verify(nodeRepo).save(nodeCap.capture());
        NodeSnapshot saved = nodeCap.getValue();
        assertThat(saved.getNodeName()).isEqualTo("n1");
        assertThat(saved.getInstanceType()).isEqualTo("t3.medium");
        assertThat(saved.getHourlyCostUsd()).isEqualByComparingTo("0.0416");
        assertThat(saved.getCpuCapacityMillicores()).isEqualTo(2000L);

        verify(podRepo, atLeastOnce()).save(any(PodSnapshot.class));

        ArgumentCaptor<NamespaceCostSnapshot> nsCap = ArgumentCaptor.forClass(NamespaceCostSnapshot.class);
        verify(nsCostRepo).save(nsCap.capture());
        assertThat(nsCap.getValue().getNamespace()).isEqualTo("default");
        assertThat(nsCap.getValue().getPodCount()).isEqualTo(2);
    }

    @Test
    void collectSkipsPricingLookupWhenInstanceTypeIsMissing() throws Exception {
        V1NodeList nodeList = new V1NodeList();
        V1Node n = new V1Node();
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName("naked-node");
        n.setMetadata(meta);
        V1NodeStatus status = new V1NodeStatus();
        status.setCapacity(Map.of("cpu", Quantity.fromString("1"), "memory", Quantity.fromString("1Gi")));
        n.setStatus(status);
        nodeList.setItems(List.of(n));
        when(coreV1Api.listNode().execute()).thenReturn(nodeList);

        V1PodList podList = new V1PodList();
        podList.setItems(List.of());
        when(coreV1Api.listPodForAllNamespaces().execute()).thenReturn(podList);

        V1DeploymentList deps = new V1DeploymentList();
        deps.setItems(List.of());
        when(appsV1Api.listDeploymentForAllNamespaces().execute()).thenReturn(deps);

        NodeMetricsList nodeMetrics = new NodeMetricsList();
        nodeMetrics.setItems(List.of());
        when(metrics.getNodeMetrics()).thenReturn(nodeMetrics);
        PodMetricsList podMetrics = new PodMetricsList();
        podMetrics.setItems(List.of());
        when(metrics.getPodMetrics(eq(""))).thenReturn(podMetrics);

        service.collect();

        ArgumentCaptor<NodeSnapshot> cap = ArgumentCaptor.forClass(NodeSnapshot.class);
        verify(nodeRepo).save(cap.capture());
        assertThat(cap.getValue().getHourlyCostUsd()).isNull();
    }
}
