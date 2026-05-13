package tech.iraelie.kubescope.domain.alertEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertEventRepository extends JpaRepository<AlertEvent, UUID> {
}
