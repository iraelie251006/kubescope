package tech.iraelie.kubescope;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Stubs every external dependency so @SpringBootTest can boot without a real
 * Kubernetes cluster, Redis, or SMTP server. The static initializer writes a
 * placeholder kubeconfig to a temp file and pins KUBECONFIG_PATH to it so the
 * production KubernetesConfig sees something valid before Spring resolves the
 * property.
 */
@TestConfiguration
public class IntegrationTestSupport {

    static {
        try {
            Path kubeconfig = Files.createTempFile("kubescope-test-kubeconfig", ".yaml");
            Files.writeString(kubeconfig, """
                    apiVersion: v1
                    kind: Config
                    clusters:
                    - name: test-cluster
                      cluster:
                        server: http://localhost:6443
                    contexts:
                    - name: test-context
                      context:
                        cluster: test-cluster
                        user: test-user
                    current-context: test-context
                    users:
                    - name: test-user
                      user: {}
                    """);
            System.setProperty("KUBECONFIG_PATH", kubeconfig.toAbsolutePath().toString());
            System.setProperty("kubescope.kubernetes.kubeconfig-path", kubeconfig.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not write placeholder kubeconfig", e);
        }
    }

    @Bean @Primary
    public ApiClient kubernetesApiClient() {
        return new ApiClient();
    }

    @Bean @Primary
    public CoreV1Api coreV1Api() {
        return mock(CoreV1Api.class);
    }

    @Bean @Primary
    public AppsV1Api appsV1Api() {
        return mock(AppsV1Api.class);
    }

    @Bean @Primary
    public Metrics metricsApi() {
        return mock(Metrics.class);
    }

    @Bean @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean @Primary
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        return template;
    }

    @Bean @Primary
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }

    @Bean @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean @Primary
    public UserDetailsService userDetailsService(
            tech.iraelie.kubescope.domain.user.UserRepository users) {
        return username -> users.findByEmail(username)
                .map(u -> (org.springframework.security.core.userdetails.UserDetails) u)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
