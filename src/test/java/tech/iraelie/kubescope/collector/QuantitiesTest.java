package tech.iraelie.kubescope.collector;

import io.kubernetes.client.custom.Quantity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuantitiesTest {

    @Test
    void cpuMillicoresReturnsNullForNullQuantity() {
        assertThat(Quantities.cpuMillicores(null)).isNull();
    }

    @Test
    void cpuMillicoresConvertsCoresToMillicores() {
        Quantity q = Quantity.fromString("2");
        assertThat(Quantities.cpuMillicores(q)).isEqualTo(2000L);
    }

    @Test
    void cpuMillicoresHandlesMillicoreInput() {
        Quantity q = Quantity.fromString("500m");
        assertThat(Quantities.cpuMillicores(q)).isEqualTo(500L);
    }

    @Test
    void memoryBytesReturnsNullForNullQuantity() {
        assertThat(Quantities.memoryBytes(null)).isNull();
    }

    @Test
    void memoryBytesConvertsKiSuffix() {
        Quantity q = Quantity.fromString("1024Ki");
        assertThat(Quantities.memoryBytes(q)).isEqualTo(1024L * 1024);
    }

    @Test
    void memoryBytesConvertsMiSuffix() {
        Quantity q = Quantity.fromString("1Mi");
        assertThat(Quantities.memoryBytes(q)).isEqualTo(1024L * 1024);
    }

    @Test
    void memoryBytesHandlesPlainNumber() {
        Quantity q = Quantity.fromString("1000000");
        assertThat(Quantities.memoryBytes(q)).isEqualTo(1_000_000L);
    }
}
