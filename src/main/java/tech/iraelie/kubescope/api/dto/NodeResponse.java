package tech.iraelie.kubescope.api.dto;

import java.math.BigDecimal;

public record NodeResponse(
        String name,
        String instanceType,
        String region,
        Long cpuCapacityMillicores,
        Long cpuUsageMillicores,
        Long memoryCapacityBytes,
        Long memoryUsageBytes,
        BigDecimal hourlyCostUsd,
        BigDecimal monthlyCostUsd) {
}

