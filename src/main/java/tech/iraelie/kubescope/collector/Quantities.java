package tech.iraelie.kubescope.collector;

import io.kubernetes.client.custom.Quantity;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Quantities {

    private Quantities() {}

    public static Long cpuMillicores(Quantity q) {
        if (q == null) return null;
        return q.getNumber()
                .multiply(BigDecimal.valueOf(1000))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public static Long memoryBytes(Quantity q) {
        if (q == null) return null;
        return q.getNumber().setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
