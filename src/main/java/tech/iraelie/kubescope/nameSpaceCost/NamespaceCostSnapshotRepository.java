package tech.iraelie.kubescope.nameSpaceCost;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NamespaceCostSnapshotRepository extends JpaRepository<NamespaceCostSnapshot, UUID> {
}
