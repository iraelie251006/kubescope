package tech.iraelie.kubescope.alertEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertEventRepository extends JpaRepository<AlertEvent, UUID> {
}
