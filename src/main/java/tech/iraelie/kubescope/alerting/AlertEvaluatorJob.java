package tech.iraelie.kubescope.alerting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kubescope.alerts.enabled", havingValue = "true", matchIfMissing = true)
public class AlertEvaluatorJob {
    private final AlertEvaluator evaluator;

    @Scheduled(
            fixedDelayString = "${kubescope.alerts.evaluation-interval-seconds:300}",
            initialDelayString = "${kubescope.alerts.evaluation-interval-seconds:300}",
            timeUnit = TimeUnit.SECONDS)
    public void run() {
        try {
            evaluator.evaluate();
        } catch (Exception e) {
            log.error("Alert evaluation failed", e);
        }
    }
}

