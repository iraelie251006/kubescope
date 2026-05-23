package tech.iraelie.kubescope.api;

import io.kubernetes.client.openapi.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.iraelie.kubescope.api.dto.ClusterOverviewResponse;
import tech.iraelie.kubescope.api.dto.DeploymentResponse;
import tech.iraelie.kubescope.api.dto.NamespaceResponse;
import tech.iraelie.kubescope.api.dto.NodeResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cluster")
public class ClusterController {

    private final ClusterReadService reads;

    @GetMapping("/overview")
    public ResponseEntity<ClusterOverviewResponse> overview() {
        ClusterOverviewResponse overviewResponse = reads.overview();
        return ResponseEntity.ok().body(overviewResponse);
    }

    @GetMapping("/nodes")
    public ResponseEntity<List<NodeResponse>> nodes() {
        List<NodeResponse> nodes = reads.nodes();
        return ResponseEntity.ok().body(nodes);
    }

    @GetMapping("/namespaces")
    public ResponseEntity<List<NamespaceResponse>> namespaces() {
        List<NamespaceResponse> namespaces = reads.namespaces();
        return ResponseEntity.ok().body(namespaces);
    }

    @GetMapping("/deployments")
    public ResponseEntity<List<DeploymentResponse>> deployments() throws ApiException {
        List<DeploymentResponse> deployments = reads.deployments();
        return ResponseEntity.ok().body(deployments);
    }
}

