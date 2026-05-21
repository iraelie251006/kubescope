package tech.iraelie.kubescope.pricing;

import java.math.BigDecimal;
import java.util.Optional;

public interface PricingService {
    Optional<BigDecimal> hourlyPriceUsd(String instanceType, String region);
}
