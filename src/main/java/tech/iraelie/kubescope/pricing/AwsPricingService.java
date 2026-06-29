package tech.iraelie.kubescope.pricing;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AwsPricingService implements PricingService {
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final ObjectProvider<PricingClient> pricingClientProvider;
    private final PricingFallback fallback;
    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    @Value("${kubescope.pricing.aws-api-enabled:false}")
    private boolean apiEnabled;

    @Override
    public Optional<BigDecimal> hourlyPriceUsd(String instanceType, String region) {
        if (instanceType == null || region == null) return Optional.empty();

        String cacheKey = "ec2-price:" + region + ":" + instanceType;
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) return Optional.of(new BigDecimal(cached));
        } catch (Exception e) {
            log.debug("Redis cache lookup failed for {}: {}", cacheKey, e.getMessage());
        }

        Optional<BigDecimal> price = Optional.empty();
        if (apiEnabled) {
            try {
                price = fetchFromAws(instanceType, region);
            } catch (Exception e) {
                log.warn("AWS pricing fetch failed for {}/{}: {}", region, instanceType, e.getMessage());
            }
        }
        if (price.isEmpty()) {
            price = fallback.lookup(instanceType, region);
        }

        price.ifPresent(p -> {
            try {
                redis.opsForValue().set(cacheKey, p.toPlainString(), CACHE_TTL);
            } catch (Exception e) {
                log.debug("Redis cache write failed for {}: {}", cacheKey, e.getMessage());
            }
        });
        return price;
    }

    private Optional<BigDecimal> fetchFromAws(String instanceType, String region) {
        PricingClient client = pricingClientProvider.getIfAvailable();
        if (client == null) return Optional.empty();

        GetProductsRequest req = GetProductsRequest.builder()
                .serviceCode("AmazonEC2")
                .filters(
                        Filter.builder().type("TERM_MATCH").field("instanceType").value(instanceType).build(),
                        Filter.builder().type("TERM_MATCH").field("regionCode").value(region).build(),
                        Filter.builder().type("TERM_MATCH").field("operatingSystem").value("Linux").build(),
                        Filter.builder().type("TERM_MATCH").field("preInstalledSw").value("NA").build(),
                        Filter.builder().type("TERM_MATCH").field("tenancy").value("Shared").build(),
                        Filter.builder().type("TERM_MATCH").field("capacitystatus").value("Used").build()
                )
                .formatVersion("aws_v1")
                .maxResults(1)
                .build();
        GetProductsResponse resp = client.getProducts(req);
        if (resp.priceList().isEmpty()) return Optional.empty();
        return parseOnDemandUsd(resp.priceList().getFirst());
    }

    Optional<BigDecimal> parseOnDemandUsd(String priceListJson) {
        try {
            JsonNode root = jsonMapper.readTree(priceListJson);
            JsonNode onDemand = root.path("terms").path("OnDemand");
            for (Map.Entry<String, JsonNode> term : onDemand.properties()) {
                JsonNode dims = term.getValue().path("priceDimensions");
                for (Map.Entry<String, JsonNode> d : dims.properties()) {
                    JsonNode usd = d.getValue().path("pricePerUnit").path("USD");
                    if (!usd.isMissingNode()) {
                        return Optional.of(new BigDecimal(usd.asText()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse pricing JSON: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
