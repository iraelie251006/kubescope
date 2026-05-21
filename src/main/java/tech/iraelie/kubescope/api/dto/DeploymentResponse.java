package tech.iraelie.kubescope.api.dto;

import java.math.BigDecimal;

public record DeploymentResponse(
        String name,
        String namespace,
        int replicas,
        Long cpuRequestMillicores,
        Long memoryRequestBytes,
        BigDecimal estimatedMonthlyCostUsd) {
}
