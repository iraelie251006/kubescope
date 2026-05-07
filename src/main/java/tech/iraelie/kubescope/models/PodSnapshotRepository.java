package tech.iraelie.kubescope.models;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PodSnapshotRepository extends JpaRepository<PodSnapshot, UUID> {

    @Query("""
            SELECT p FROM PodSnapshot p
            WHERE p.timestamp = (SELECT MAX(p2.timestamp) FROM PodSnapshot p2)
            """)
    List<PodSnapshot> findLatestSnapshot();
}
