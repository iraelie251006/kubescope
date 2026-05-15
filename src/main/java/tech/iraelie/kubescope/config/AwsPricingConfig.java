package tech.iraelie.kubescope.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pricing.PricingClient;

@Configuration
@ConditionalOnProperty(name = "kubescope.pricing.aws-api-enabled", havingValue = "true")
public class AwsPricingConfig {

    @Value("${kubescope.pricing.aws-region:us-east-1}")
    private String region;

    @Bean
    public PricingClient pricingClient() {
        // Pricing API is regional (us-east-1 / ap-south-1).
        return PricingClient.builder().region(Region.of(region)).build();
    }
}
