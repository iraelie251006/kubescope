package tech.iraelie.kubescope.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileReader;
import java.io.IOException;

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
        return Config.defaultClient();
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
