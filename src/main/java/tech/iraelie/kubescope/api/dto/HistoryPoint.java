package tech.iraelie.kubescope.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoryPoint(
        Instant timestamp,
        BigDecimal totalHourlyCostUsd,
        BigDecimal totalMonthlyCostUsd,
        Long totalCpuUsageMillicores,
        Long totalMemoryUsageBytes,
        Long totalCpuCapacityMillicores,
        Long totalMemoryCapacityBytes) {
}
