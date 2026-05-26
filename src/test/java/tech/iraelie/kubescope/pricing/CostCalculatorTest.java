package tech.iraelie.kubescope.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CostCalculatorTest {

    @Test
    void monthlyFromHourlyMultipliesBy720AndRoundsToTwoPlaces() {
        BigDecimal hourly = new BigDecimal("0.0832");

        BigDecimal monthly = CostCalculator.monthlyFromHourly(hourly);

        // 0.0832 * 720 = 59.904 -> 59.90 (HALF_UP)
        assertThat(monthly).isEqualByComparingTo(new BigDecimal("59.90"));
    }

    @Test
    void monthlyFromHourlyReturnsZeroForNull() {
        assertThat(CostCalculator.monthlyFromHourly(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void dailyFromHourlyMultipliesBy24AndRoundsToTwoPlaces() {
        BigDecimal hourly = new BigDecimal("0.10");

        BigDecimal daily = CostCalculator.dailyFromHourly(hourly);

        assertThat(daily).isEqualByComparingTo(new BigDecimal("2.40"));
    }

    @Test
    void dailyFromHourlyReturnsZeroForNull() {
        assertThat(CostCalculator.dailyFromHourly(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void proportionalShareReturnsZeroWhenTotalIsZero() {
        assertThat(CostCalculator.proportionalShare(100, 0)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void proportionalShareReturnsZeroWhenPartIsZero() {
        assertThat(CostCalculator.proportionalShare(0, 100)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void proportionalShareReturnsCorrectFraction() {
        BigDecimal share = CostCalculator.proportionalShare(25, 100);
        assertThat(share).isEqualByComparingTo(new BigDecimal("0.25000000"));
    }

    @Test
    void blendedShareAveragesCpuAndMemoryShares() {
        BigDecimal share = CostCalculator.blendedShare(50, 100, 100, 100);
        // (0.5 + 1.0) / 2 = 0.75
        assertThat(share).isEqualByComparingTo(new BigDecimal("0.75000000"));
    }

    @Test
    void applyShareMultipliesAndRoundsToTwoPlaces() {
        BigDecimal result = CostCalculator.applyShare(new BigDecimal("100.00"), new BigDecimal("0.333333"));
        // 100 * 0.333333 = 33.3333 -> 33.33
        assertThat(result).isEqualByComparingTo(new BigDecimal("33.33"));
    }

    @Test
    void applyShareReturnsZeroOnNullInputs() {
        assertThat(CostCalculator.applyShare(null, new BigDecimal("0.5"))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(CostCalculator.applyShare(new BigDecimal("100"), null)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
