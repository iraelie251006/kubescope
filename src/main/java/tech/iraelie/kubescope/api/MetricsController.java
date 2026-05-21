package tech.iraelie.kubescope.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.iraelie.kubescope.api.dto.HistoryPoint;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final ClusterReadService reads;

    @GetMapping("/history")
    public List<HistoryPoint> history(@RequestParam(defaultValue = "24h") String range) {
        return reads.history(range);
    }
}
