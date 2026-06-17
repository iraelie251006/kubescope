package tech.iraelie.kubescope.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.iraelie.kubescope.api.dto.AlertRuleResponse;
import tech.iraelie.kubescope.api.dto.CreateAlertRuleRequest;
import tech.iraelie.kubescope.domain.alertRule.AlertRule;
import tech.iraelie.kubescope.domain.alertRule.AlertRuleRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/alerts")
public class AlertController {
    private final AlertRuleRepository repo;

    @GetMapping
    public ResponseEntity<List<AlertRuleResponse>> list() {
        List<AlertRuleResponse> alertRuleResponseList =
                repo.findAll().stream().map(AlertRuleResponse::of).toList();
        return ResponseEntity.ok().body(alertRuleResponseList);
    }

    @PostMapping
    public ResponseEntity<AlertRuleResponse> create(@Valid @RequestBody CreateAlertRuleRequest req) {
        AlertRule rule = new AlertRule();
        rule.setMetricType(req.metricType());
        rule.setCondition(req.condition());
        rule.setThresholdValue(req.thresholdValue());
        rule.setNotificationEmail(req.notificationEmail());
        AlertRule saved = repo.save(rule);

        AlertRuleResponse response = AlertRuleResponse.of(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
