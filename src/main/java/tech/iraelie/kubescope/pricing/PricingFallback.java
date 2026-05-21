package tech.iraelie.kubescope.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class PricingFallback {
    private final ObjectMapper objectMapper;
    private Map<String, Map<String, BigDecimal>> table = Map.of();

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource("pricing/ec2-fallback.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            JsonNode regions = root.path("regions");
            Map<String, Map<String, BigDecimal>> built = new HashMap<>();
            for (Map.Entry<String, JsonNode> region : regions.properties()) {
                Map<String, BigDecimal> byType = new HashMap<>();
                for (Map.Entry<String, JsonNode> t : region.getValue().properties()) {
                    byType.put(t.getKey(), new BigDecimal(t.getValue().asText()));
                }
                built.put(region.getKey(), byType);
            }
            this.table = built;
            log.info("Loaded EC2 pricing fallback: {} regions", table.size());
        } catch (Exception e) {
            log.error("Failed to load pricing fallback resource", e);
        }
    }

    public Optional<BigDecimal> lookup(String instanceType, String region) {
        if (instanceType == null || region == null) return Optional.empty();
        Map<String, BigDecimal> byType = table.get(region);
        if (byType == null) return Optional.empty();
        return Optional.ofNullable(byType.get(instanceType));
    }
}
