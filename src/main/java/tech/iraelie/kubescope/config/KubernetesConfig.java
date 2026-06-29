package tech.iraelie.kubescope.config;

import io.kubernetes.client.Metrics;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileReader;
import java.io.IOException;

@Slf4j
@Configuration
public class KubernetesConfig {

    @Value("${kubescope.kubernetes.kubeconfig-path:}")
    private String kubeconfigPath;

    @Value("${kubescope.kubernetes.in-cluster:false}")
    private boolean inCluster;

    @Bean
    public ApiClient kubernetesApiClient() throws IOException {
        if (inCluster) {
            return ClientBuilder.cluster().build();
        }
        if (kubeconfigPath != null && !kubeconfigPath.isBlank()) {
            try (FileReader reader = new FileReader(kubeconfigPath)) {
                return ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(reader)).build();
            }
        }
        try {
            return Config.defaultClient();
        } catch (IllegalArgumentException | IOException e) {
            log.warn("No usable kubeconfig found ({}). Returning a stub ApiClient — " +
                    "set IN_CLUSTER=true or KUBECONFIG_PATH for real cluster access, " +
                    "or COLLECTOR_ENABLED=false to silence collector errors.", e.getMessage());
            return new ApiClient();
        }
    }

    @Bean
    public CoreV1Api coreV1Api(ApiClient apiClient) {
        return new CoreV1Api(apiClient);
    }

    @Bean
    public AppsV1Api appsV1Api(ApiClient apiClient) {
        return new AppsV1Api(apiClient);
    }

    @Bean
    public Metrics metricsApi(ApiClient apiClient) {
        return new Metrics(apiClient);
    }
}
