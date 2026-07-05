package tech.iraelie.kubescope.pricing;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PricingFallbackTest {

    private PricingFallback fallback;

    @BeforeEach
    void setUp() throws Exception {
        fallback = new PricingFallback(new ObjectMapper());
        Method load = PricingFallback.class.getDeclaredMethod("load");
        load.setAccessible(true);
        load.invoke(fallback);
    }

    @Test
    void lookupReturnsPriceForKnownInstanceAndRegion() {
        Optional<BigDecimal> price = fallback.lookup("t3.medium", "us-east-1");

        assertThat(price).isPresent();
        assertThat(price.get()).isEqualByComparingTo("0.0416");
    }

    @Test
    void lookupReturnsEmptyForUnknownInstance() {
        Optional<BigDecimal> price = fallback.lookup("nope.huge", "us-east-1");

        assertThat(price).isEmpty();
    }

    @Test
    void lookupReturnsEmptyForUnknownRegion() {
        Optional<BigDecimal> price = fallback.lookup("t3.medium", "mars-1");

        assertThat(price).isEmpty();
    }

    @Test
    void lookupReturnsEmptyForNullInputs() {
        assertThat(fallback.lookup(null, "us-east-1")).isEmpty();
        assertThat(fallback.lookup("t3.medium", null)).isEmpty();
    }

    @Test
    void differentRegionsCanCarryDifferentPrices() {
        BigDecimal east = fallback.lookup("t3.medium", "us-east-1").orElseThrow();
        BigDecimal eu = fallback.lookup("t3.medium", "eu-west-1").orElseThrow();

        assertThat(east).isNotEqualByComparingTo(eu);
    }

    @Test
    void loadFailureLeavesTableEmpty() {
        // Replace the loaded table with an empty one to simulate a load failure path
        ReflectionTestUtils.setField(fallback, "table", java.util.Map.of());

        assertThat(fallback.lookup("t3.medium", "us-east-1")).isEmpty();
    }
}
