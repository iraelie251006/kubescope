package tech.iraelie.kubescope.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AwsPricingServiceTest {

    @Mock private ObjectProvider<PricingClient> pricingClientProvider;
    @Mock private PricingClient pricingClient;
    @Mock private PricingFallback fallback;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private AwsPricingService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new AwsPricingService(pricingClientProvider, fallback, redis, mapper);
        ReflectionTestUtils.setField(service, "apiEnabled", false);
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void returnsEmptyOnNullInputs() {
        assertThat(service.hourlyPriceUsd(null, "us-east-1")).isEmpty();
        assertThat(service.hourlyPriceUsd("t3.medium", null)).isEmpty();
    }

    @Test
    void returnsCachedValueWhenPresent() {
        when(valueOps.get("ec2-price:us-east-1:t3.medium")).thenReturn("0.0416");

        Optional<BigDecimal> price = service.hourlyPriceUsd("t3.medium", "us-east-1");

        assertThat(price).isPresent();
        assertThat(price.get()).isEqualByComparingTo("0.0416");
        verify(fallback, never()).lookup(any(), any());
    }

    @Test
    void fallsBackToStaticTableWhenCacheMissesAndApiDisabled() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(fallback.lookup("t3.medium", "us-east-1")).thenReturn(Optional.of(new BigDecimal("0.0416")));

        Optional<BigDecimal> price = service.hourlyPriceUsd("t3.medium", "us-east-1");

        assertThat(price).isPresent();
        assertThat(price.get()).isEqualByComparingTo("0.0416");
        // The resolved price is then written back to the cache
        verify(valueOps).set(eq("ec2-price:us-east-1:t3.medium"), eq("0.0416"), any(Duration.class));
    }

    @Test
    void redisCacheReadFailureFallsThroughToFallback() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(fallback.lookup("t3.medium", "us-east-1")).thenReturn(Optional.of(new BigDecimal("0.0416")));

        Optional<BigDecimal> price = service.hourlyPriceUsd("t3.medium", "us-east-1");

        assertThat(price).isPresent();
    }

    @Test
    void apiEnabledButClientUnavailableFallsBackToTable() {
        ReflectionTestUtils.setField(service, "apiEnabled", true);
        when(valueOps.get(anyString())).thenReturn(null);
        when(pricingClientProvider.getIfAvailable()).thenReturn(null);
        when(fallback.lookup("t3.medium", "us-east-1")).thenReturn(Optional.of(new BigDecimal("0.05")));

        Optional<BigDecimal> price = service.hourlyPriceUsd("t3.medium", "us-east-1");

        assertThat(price).isPresent();
        assertThat(price.get()).isEqualByComparingTo("0.05");
    }

    @Test
    void apiEnabledAndClientReturnsPriceShortCircuitsFallback() {
        ReflectionTestUtils.setField(service, "apiEnabled", true);
        when(valueOps.get(anyString())).thenReturn(null);
        when(pricingClientProvider.getIfAvailable()).thenReturn(pricingClient);
        String priceJson = """
                {"terms":{"OnDemand":{"abc":{"priceDimensions":{"d1":{"pricePerUnit":{"USD":"0.0700"}}}}}}}
                """;
        GetProductsResponse resp = GetProductsResponse.builder().priceList(List.of(priceJson)).build();
        when(pricingClient.getProducts(any(GetProductsRequest.class))).thenReturn(resp);

        Optional<BigDecimal> price = service.hourlyPriceUsd("t3.medium", "us-east-1");

        assertThat(price).isPresent();
        assertThat(price.get()).isEqualByComparingTo("0.0700");
        verify(fallback, never()).lookup(any(), any());
        verify(pricingClient, times(1)).getProducts(any(GetProductsRequest.class));
    }

    @Test
    void parseOnDemandUsdReturnsEmptyOnBadJson() {
        Optional<BigDecimal> price = service.parseOnDemandUsd("{not valid json");
        assertThat(price).isEmpty();
    }

    @Test
    void parseOnDemandUsdExtractsFirstPriceDimension() {
        String priceJson = """
                {"terms":{"OnDemand":{"abc":{"priceDimensions":{"d1":{"pricePerUnit":{"USD":"0.0192"}}}}}}}
                """;
        Optional<BigDecimal> price = service.parseOnDemandUsd(priceJson);

        assertThat(price).isPresent();
        assertThat(price.get()).isEqualByComparingTo("0.0192");
    }
}
