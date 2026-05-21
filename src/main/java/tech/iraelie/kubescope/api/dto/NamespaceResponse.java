package tech.iraelie.kubescope.api.dto;

import java.math.BigDecimal;

public record NamespaceResponse(
        String namespace,
        int podCount,
        Long cpuUsageMillicores,
        Long memoryUsageBytes,
        BigDecimal estimatedMonthlyCostUsd) {
}
