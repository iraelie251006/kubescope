package tech.iraelie.kubescope.domain.podSnapshot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class PodSnapshotRepositoryTest {

    @Autowired private PodSnapshotRepository repo;

    private PodSnapshot pod(String name, String ns, Instant ts) {
        return PodSnapshot.builder()
                .podName(name)
                .namespace(ns)
                .timestamp(ts)
                .build();
    }

    @Test
    void findLatestSnapshotReturnsOnlyMaxTimestampRows() {
        Instant older = Instant.parse("2025-01-01T00:00:00Z");
        Instant newer = Instant.parse("2025-01-02T00:00:00Z");
        repo.save(pod("p-old-1", "default", older));
        repo.save(pod("p-old-2", "default", older));
        repo.save(pod("p-new-1", "default", newer));
        repo.save(pod("p-new-2", "kube-system", newer));

        List<PodSnapshot> latest = repo.findLatestSnapshot();

        assertThat(latest).hasSize(2);
        assertThat(latest).allMatch(s -> s.getTimestamp().equals(newer));
    }

    @Test
    void findLatestSnapshotReturnsEmptyWhenNoRows() {
        assertThat(repo.findLatestSnapshot()).isEmpty();
    }
}
