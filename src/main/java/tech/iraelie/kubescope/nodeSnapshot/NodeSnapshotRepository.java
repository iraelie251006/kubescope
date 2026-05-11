package tech.iraelie.kubescope.nodeSnapshot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NodeSnapshotRepository extends JpaRepository<NodeSnapshot, UUID> {

    @Query("""
            SELECT n FROM NodeSnapshot n
            WHERE n.timestamp = (SELECT MAX(n2.timestamp) FROM NodeSnapshot n2)
            """)
    List<NodeSnapshot> findLatestSnapshot();

    List<NodeSnapshot> findByTimestampGreaterThanEqualOrderByTimestampDesc(Instant since);
}

