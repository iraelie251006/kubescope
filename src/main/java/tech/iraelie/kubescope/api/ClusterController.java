package tech.iraelie.kubescope.api;

import io.kubernetes.client.openapi.ApiException;
import lombok.RequiredArgsConstructor;
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
    public ClusterOverviewResponse overview() {
        return reads.overview();
    }

    @GetMapping("/nodes")
    public List<NodeResponse> nodes() {
        return reads.nodes();
    }

    @GetMapping("/namespaces")
    public List<NamespaceResponse> namespaces() {
        return reads.namespaces();
    }

    @GetMapping("/deployments")
    public List<DeploymentResponse> deployments() throws ApiException {
        return reads.deployments();
    }
}

