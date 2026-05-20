package tech.iraelie.kubescope.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "kubescope.collector.enabled", havingValue = "true", matchIfMissing = true)
public class MetricsCollectorJob {

    private static final Logger log = LoggerFactory.getLogger(MetricsCollectorJob.class);

    private final MetricsCollectorService service;

    public MetricsCollectorJob(MetricsCollectorService service) {
        this.service = service;
    }

    @Scheduled(
            fixedDelayString = "${kubescope.collector.interval-seconds:60}",
            initialDelayString = "${kubescope.collector.interval-seconds:60}",
            timeUnit = TimeUnit.SECONDS)
    public void run() {
        long start = System.currentTimeMillis();
        try {
            service.collect();
            log.debug("Metrics collection finished in {} ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Metrics collection failed", e);
        }
    }
}
