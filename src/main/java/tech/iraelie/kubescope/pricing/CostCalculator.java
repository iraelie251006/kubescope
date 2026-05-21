package tech.iraelie.kubescope.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CostCalculator {

    public static final BigDecimal HOURS_PER_DAY = BigDecimal.valueOf(24);
    public static final BigDecimal HOURS_PER_MONTH = BigDecimal.valueOf(24L * 30L);
    private static final int SHARE_SCALE = 8;

    private CostCalculator() {}

    public static BigDecimal monthlyFromHourly(BigDecimal hourly) {
        if (hourly == null) return BigDecimal.ZERO;
        return hourly.multiply(HOURS_PER_MONTH).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal dailyFromHourly(BigDecimal hourly) {
        if (hourly == null) return BigDecimal.ZERO;
        return hourly.multiply(HOURS_PER_DAY).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal proportionalShare(long part, long total) {
        if (total <= 0 || part <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part).divide(BigDecimal.valueOf(total), SHARE_SCALE, RoundingMode.HALF_UP);
    }

    // Blended share weighting CPU and memory equally.
    public static BigDecimal blendedShare(long partCpu, long totalCpu, long partMem, long totalMem) {
        BigDecimal cpu = proportionalShare(partCpu, totalCpu);
        BigDecimal mem = proportionalShare(partMem, totalMem);
        return cpu.add(mem).divide(BigDecimal.valueOf(2), SHARE_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal applyShare(BigDecimal totalCost, BigDecimal share) {
        if (totalCost == null || share == null) return BigDecimal.ZERO;
        return totalCost.multiply(share).setScale(2, RoundingMode.HALF_UP);
    }
}
