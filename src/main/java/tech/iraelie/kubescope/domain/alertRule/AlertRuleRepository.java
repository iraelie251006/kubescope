package tech.iraelie.kubescope.domain.alertRule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {
}
