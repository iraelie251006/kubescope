package tech.iraelie.kubescope.nameSpaceCost;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NamespaceCostSnapshotRepository extends JpaRepository<NamespaceCostSnapshot, UUID> {
    List<NamespaceCostSnapshot> findByTimestampGreaterThanEqualOrderByTimestampDesc(Instant since);
}
