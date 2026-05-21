package tech.iraelie.kubescope.api.dto;

import java.math.BigDecimal;

public record ClusterOverviewResponse(
        int totalNodes,
        int totalPods,
        BigDecimal cpuUsagePercent,
        BigDecimal memoryUsagePercent,
        BigDecimal estimatedMonthlyCostUsd) {
}

