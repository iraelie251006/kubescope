package tech.iraelie.kubescope;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationTestSupport.class)
class KubescopeApplicationTests {

    @Test
    void contextLoads() {
        // Full Spring context boots with external dependencies stubbed via
        // IntegrationTestSupport. Failure here means a misconfigured bean or
        // a property that the app needs but the test profile doesn't supply.
    }
}
